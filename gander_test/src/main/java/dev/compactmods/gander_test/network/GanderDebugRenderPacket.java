package dev.compactmods.gander_test.network;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;

import dev.compactmods.gander.runtime.additions.ModelManager$Gander;
import dev.compactmods.gander.runtime.baked.AtlasIndex;
import dev.compactmods.gander.runtime.baked.section.SectionBaker;
import dev.compactmods.gander.runtime.baked.section.SectionBaker.BakedSection;
import dev.compactmods.gander.runtime.baked.section.SectionBaker.DrawCall;
import dev.compactmods.gander_test.GanderTestMod;
import dev.compactmods.gander.runtime.baked.model.ModelRebaker;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL43;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Objects;

import static org.lwjgl.system.MemoryStack.stackPush;

public record GanderDebugRenderPacket(BlockState state) implements CustomPacketPayload
{
    private static Logger LOGGER = LoggerFactory.getLogger(GanderDebugRenderPacket.class);

    private static boolean render = false;

    public static final Type<GanderDebugRenderPacket> ID = new Type<>(GanderTestMod.asResource("debug"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GanderDebugRenderPacket> STREAM_CODEC
        = StreamCodec.composite(
        ResourceKey.streamCodec(Registries.BLOCK), pkt -> pkt.state().getBlockHolder().unwrapKey().get(),
        key -> new GanderDebugRenderPacket(Objects.requireNonNull(BuiltInRegistries.BLOCK.getValue(key)).defaultBlockState()));

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return ID;
    }

    public static void handle(GanderDebugRenderPacket pkt, IPayloadContext ctx)
    {
        if (FMLEnvironment.dist.isClient())
        {
            render = !render;
            //lastBlockState = pkt.state();
        }
    }

    private static boolean builtBuffers = false;
    private static int drawBuffer;
    private static int drawCount;
    private static int vertexArray;
    private static int transformTexture;
    private static int spriteTexture;
    private static int atlasBuffer;
    private static final IntList buffers = new IntArrayList();
    private static final IntList textures = new IntArrayList();

    private static void buildBuffers(BakedSection section)
    {
        vertexArray = GL32.glGenVertexArrays();
        GL32.glBindVertexArray(vertexArray);

        var indexBuffer = buildIndexBuffer(section);
        var vertexBuffer = buildVertexBuffer(section);
        var normalBuffer = buildNormalBuffer(section);
        var uvBuffer = buildUvBuffer(section);
        var transformOffsetBuffer = buildTransformOffsetBuffer(section);
        var spriteOffsetBuffer = buildSpriteOffsetBuffer(section);

        GL32.glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);

        GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vertexBuffer);
        GL32.glVertexAttribPointer(0, 3, GL32.GL_FLOAT, false, 0, 0);
        GL32.glEnableVertexAttribArray(0);

        GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, normalBuffer);
        GL32.glVertexAttribPointer(1, 3, GL32.GL_FLOAT, false, 0, 0);
        GL32.glEnableVertexAttribArray(1);

        GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, uvBuffer);
        GL32.glVertexAttribPointer(2, 2, GL32.GL_FLOAT, false, 0, 0);
        GL32.glEnableVertexAttribArray(2);

        GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, transformOffsetBuffer);
        GL32.glVertexAttribIPointer(3, 1, GL32.GL_UNSIGNED_SHORT, 0, 0);
        GL32.glEnableVertexAttribArray(3);

        GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, spriteOffsetBuffer);
        GL32.glVertexAttribIPointer(4, 1, GL32.GL_UNSIGNED_SHORT, 0, 0);
        GL32.glEnableVertexAttribArray(4);

        transformTexture = buildTransformTexture(section);
        spriteTexture = buildSpriteTexture(section);
        atlasBuffer = buildAtlasBuffer(section);

        drawBuffer = buildDrawBuffer(section);
        drawCount = section.drawCalls().size();
    }

    private static int buildVertexBuffer(BakedSection section)
    {
        int count = 0;
        for (var call : section.drawCalls())
        {
            count += call.mesh().vertices().limit();
        }

        var vertexBuffer = GL32.glGenBuffers();
        GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vertexBuffer);
        GL32.glBufferData(GL32.GL_ARRAY_BUFFER, count * 4, GL32.GL_STATIC_DRAW);
        buffers.add(vertexBuffer);

        int index = 0;
        for (var call : section.drawCalls())
        {
            var mesh = call.mesh();
            try (var stack = stackPush())
            {
                var vertices = stack.mallocFloat(mesh.vertices().limit());
                vertices.put(mesh.vertices());
                vertices.flip();
                mesh.vertices().rewind();

                GL32.glBufferSubData(GL32.GL_ARRAY_BUFFER, index * 4, vertices);
            }

            index += mesh.vertices().limit();
        }

        return vertexBuffer;
    }

    private static int buildIndexBuffer(BakedSection section)
    {
        int count = 0;
        for (var call : section.drawCalls())
        {
            count += call.mesh().indices().limit();
        }

        var indexBuffer = GL32.glGenBuffers();
        GL32.glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        GL32.glBufferData(GL32.GL_ELEMENT_ARRAY_BUFFER, count, GL32.GL_STATIC_DRAW);
        buffers.add(indexBuffer);

        var index = 0;
        for (var call : section.drawCalls())
        {
            var mesh = call.mesh();
            try (var stack = stackPush())
            {
                var indices = stack.malloc(mesh.indices().limit());
                indices.put(mesh.indices());
                indices.flip();
                mesh.indices().rewind();

                GL32.glBufferSubData(GL32.GL_ELEMENT_ARRAY_BUFFER, index, indices);
            }

            index += mesh.indices().limit();
        }

        return indexBuffer;
    }

    private static int buildNormalBuffer(BakedSection section)
    {
        int count = 0;
        for (var call : section.drawCalls())
        {
            count += call.mesh().normals().limit();
        }

        var normalBuffer = GL32.glGenBuffers();
        GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, normalBuffer);
        GL32.glBufferData(GL32.GL_ARRAY_BUFFER, count * 4, GL32.GL_STATIC_DRAW);
        buffers.add(normalBuffer);

        int index = 0;
        for (var call : section.drawCalls())
        {
            var mesh = call.mesh();
            try (var stack = stackPush())
            {
                var normals = stack.mallocFloat(mesh.normals().limit());
                normals.put(mesh.normals());
                normals.flip();
                mesh.normals().rewind();

                GL32.glBufferSubData(GL32.GL_ARRAY_BUFFER, index * 4, normals);
            }

            index += mesh.normals().limit();
        }

        return normalBuffer;
    }

    private static int buildUvBuffer(BakedSection section)
    {
        int count = 0;
        for (var call : section.drawCalls())
        {
            count += call.mesh().uvs().limit();
        }

        var uvBuffer = GL32.glGenBuffers();
        GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, uvBuffer);
        GL32.glBufferData(GL32.GL_ARRAY_BUFFER, count * 4, GL32.GL_STATIC_DRAW);
        buffers.add(uvBuffer);

        int index = 0;
        for (var call : section.drawCalls())
        {
            var mesh = call.mesh();
            try (var stack = stackPush())
            {
                var uvs = stack.mallocFloat(mesh.uvs().limit());
                uvs.put(mesh.uvs());
                uvs.flip();
                mesh.uvs().rewind();

                GL32.glBufferSubData(GL32.GL_ARRAY_BUFFER, index * 4, uvs);
            }

            index += mesh.uvs().limit();
        }

        return uvBuffer;
    }

    private static int buildSpriteTexture(BakedSection section)
    {
        int count = 0;
        for (var call : section.drawCalls())
        {
            count += call.textures().limit();
        }

        var width = Math.min(count, GL32.glGetInteger(GL32.GL_MAX_TEXTURE_SIZE));
        var height = (int)Math.ceil((double)count / width);

        var spriteTexture = GL32.glGenTextures();
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, spriteTexture);
        GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_NEAREST);
        GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_NEAREST);
        GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_S, GL32.GL_CLAMP_TO_EDGE);
        GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_T, GL32.GL_CLAMP_TO_EDGE);
        GL32.glPixelStorei(GL32.GL_UNPACK_ALIGNMENT, 1);

        GL32.glTexImage2D(GL32.GL_TEXTURE_2D,
            0,
            GL32.GL_R32I,
            width, height,
            0,
            GL32.GL_RED_INTEGER,
            GL32.GL_INT,
            (ByteBuffer)null);
        textures.add(spriteTexture);

        // TODO: texture building should be extracted into a common function
        int indexX = 0;
        int indexY = 0;
        for (var call : section.drawCalls())
        {
            var toWrite = call.textures().limit();
            var valuesWritten = 0;
            while (valuesWritten < toWrite)
            {
                // Values to write in *this* row
                var inThisRow = Math.min(width - indexX, toWrite - valuesWritten);

                call.textures().position(valuesWritten);
                GL32.glPixelStorei(GL32.GL_UNPACK_ALIGNMENT, 1);
                GL32.glTexSubImage2D(
                    GL32.GL_TEXTURE_2D,
                    0,
                    indexX,
                    indexY,
                    inThisRow,
                    1,
                    GL32.GL_RED_INTEGER,
                    GL32.GL_INT,
                    call.textures());
                //call.textures().rewind();

                if ((valuesWritten + inThisRow) < toWrite)
                {
                    // If there's data remaining, that's because we overflowed.
                    indexX = 0;
                    indexY++;
                }
                else
                {
                    // Otherwise, add the amount we wrote now.
                    indexX += inThisRow;
                }

                valuesWritten += inThisRow;
            }
        }

        return spriteTexture;
    }

    private static int buildTransformOffsetBuffer(BakedSection section)
    {
        int count = 0;
        for (var call : section.drawCalls())
        {
            count += call.mesh().vertexCount();
        }

        var transformIndexBuffer = GL32.glGenBuffers();
        GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, transformIndexBuffer);
        GL32.glBufferData(GL32.GL_ARRAY_BUFFER, count * 2, GL32.GL_STATIC_DRAW);
        buffers.add(transformIndexBuffer);

        var index = 0;
        var transformOffset = 0;
        for (var call : section.drawCalls())
        {
            try (var stack = stackPush())
            {
                var transforms = stack.mallocShort(call.mesh().vertexCount());
                // I am sobbing. WHY is there no fill(T) style method????
                for (int x = 0; x < call.mesh().vertexCount(); x++)
                    transforms.put((short)transformOffset);

                transforms.flip();
                GL32.glBufferSubData(GL32.GL_ARRAY_BUFFER, index * 2, transforms);
            }

            index += call.mesh().vertexCount();
            transformOffset += call.instanceCount();
        }

        return transformIndexBuffer;
    }

    private static int buildSpriteOffsetBuffer(BakedSection section)
    {
        int count = 0;
        for (var call : section.drawCalls())
        {
            count += call.mesh().vertexCount();
        }


        var spriteIndexBuffer = GL32.glGenBuffers();
        GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, spriteIndexBuffer);
        GL32.glBufferData(GL32.GL_ARRAY_BUFFER, count * 2, GL32.GL_STATIC_DRAW);
        buffers.add(spriteIndexBuffer);

        var index = 0;
        var spriteOffset = 0;
        for (var call : section.drawCalls())
        {
            try (var stack = stackPush())
            {
                var sprites = stack.mallocShort(call.mesh().vertexCount());
                // I am sobbing. WHY is there no fill(T) style method????
                for (int x = 0; x < call.mesh().vertexCount(); x++)
                    sprites.put((short)spriteOffset);

                sprites.flip();
                GL32.glBufferSubData(GL32.GL_ARRAY_BUFFER, index * 2, sprites);
            }

            index += call.mesh().vertexCount();
            spriteOffset += call.textureCount();
        }

        return spriteIndexBuffer;
    }

    private static int buildTransformTexture(BakedSection section)
    {
        int count = 0;
        for (var call : section.drawCalls())
        {
            count += call.transforms().limit();
        }

        // Dividing by 4 here makes sure we can always fit an integer number
        // of matrices if we have more than one row of data.
        var width = Math.min(count / 4, GL32.glGetInteger(GL32.GL_MAX_TEXTURE_SIZE) / 4);
        var height = (int)Math.ceil(((double)count / 4) / width);

        var transformTexture = GL32.glGenTextures();
        GL32.glBindTexture(GL32.GL_TEXTURE_2D, transformTexture);
        GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_NEAREST);
        GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_NEAREST);
        GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_S, GL32.GL_CLAMP_TO_EDGE);
        GL32.glTexParameteri(GL32.GL_TEXTURE_2D, GL32.GL_TEXTURE_WRAP_T, GL32.GL_CLAMP_TO_EDGE);
        GL32.glPixelStorei(GL32.GL_UNPACK_ALIGNMENT, 1);
        GL32.glTexImage2D(GL32.GL_TEXTURE_2D,
            0,
            GL32.GL_RGBA32F,
            width, height,
            0,
            GL32.GL_RGBA,
            GL32.GL_FLOAT,
            (ByteBuffer)null);
        textures.add(transformTexture);

        // TODO: texture building should be extracted into a common function
        int indexX = 0;
        int indexY = 0;
        for (var call : section.drawCalls())
        {
            var toWrite = call.transforms().limit();
            var pixelCount = toWrite / 4;
            var valuesWritten = 0;
            var pixelsWritten = 0;
            while (valuesWritten < toWrite)
            {
                var inThisRow = Math.min(width - indexX, pixelCount - pixelsWritten);

                call.transforms().position(valuesWritten);
                GL32.glPixelStorei(GL32.GL_UNPACK_ALIGNMENT, 1);
                GL32.glTexSubImage2D(
                    GL32.GL_TEXTURE_2D,
                    0,
                    indexX,
                    indexY,
                    inThisRow,
                    1,
                    GL32.GL_RGBA,
                    GL32.GL_FLOAT,
                    call.transforms());
                //call.transforms().rewind();

                if ((valuesWritten + inThisRow * 4) < toWrite)
                {
                    // If there's data remaining, that's because we overflowed.
                    indexX = 0;
                    indexY++;
                }
                else
                {
                    indexX += inThisRow;
                }

                valuesWritten += inThisRow * 4;
                pixelsWritten += inThisRow;
            }
        }

        return transformTexture;
    }

    private static int buildAtlasBuffer(BakedSection section)
    {
        int count = 0;
        count += section.atlas().limit();

        var spriteBuffer = GL32.glGenBuffers();
        GL32.glBindBuffer(GL32.GL_UNIFORM_BUFFER, spriteBuffer);
        GL32.glBufferData(GL32.GL_UNIFORM_BUFFER, 4 * count, GL32.GL_STATIC_DRAW);

        buffers.add(spriteBuffer);
        var index = 0;
        GL32.glBufferSubData(GL32.GL_UNIFORM_BUFFER, index, section.atlas());
        index += section.atlas().limit();

        return spriteBuffer;
    }

    private static int buildDrawBuffer(BakedSection section)
    {
        var drawBuffer = GL32.glGenBuffers();
        GL32.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, drawBuffer);
        buffers.add(drawBuffer);

        try (var stack = stackPush())
        {
            var index = 0;
            var vertexOffset = 0;
            var drawCalls = stack.mallocInt(5 * section.drawCalls().size());
            for (var drawCall : section.drawCalls())
            {
                // count
                drawCalls.put(drawCall.mesh().indices().limit());
                // instanceCount
                drawCalls.put(drawCall.instanceCount());
                // firstIndex
                drawCalls.put(index);
                // baseVertex
                drawCalls.put(vertexOffset);
                // baseInstance
                drawCalls.put(0);
                index += drawCall.mesh().indices().limit();
                vertexOffset += drawCall.mesh().vertexCount();
            }

            drawCalls.flip();

            GL32.glBufferData(GL40.GL_DRAW_INDIRECT_BUFFER, drawCalls, GL32.GL_STATIC_DRAW);
        }

        return drawBuffer;
    }

    private static int shader;
    private static Uniform projectionMatrix;
    private static Uniform modelViewMatrix;
    private static Uniform transformSampler;
    private static Uniform spriteSampler;
    private static Uniform blockAtlas;

    private static final IntList shaders = new IntArrayList();

    private static void buildShader(BakedSection section)
    {
        projectionMatrix = new Uniform("ProjMat", Uniform.UT_MAT4, 16);
        modelViewMatrix = new Uniform("ModelViewMat", Uniform.UT_MAT4, 16);
        transformSampler = new Uniform("TransformSampler", Uniform.UT_INT1, 1);
        spriteSampler = new Uniform("SpriteSampler", Uniform.UT_INT1, 1);
        blockAtlas = new Uniform("BlockAtlas", Uniform.UT_INT1, 1);

        var atlasSize = section.atlas().limit() / 4;
        var transformCount = section.drawCalls().stream()
            .mapToInt(DrawCall::transformCount)
            .sum();
        var spriteCount = section.drawCalls().stream()
            .mapToInt(DrawCall::textureCount)
            .sum();

        var vertex = GL32.glCreateShader(GL32.GL_VERTEX_SHADER);
        shaders.add(vertex);
        // TODO: this requires 3.3...
        GL32.glShaderSource(vertex, """
            #version 460

            #define ATLAS_SIZE %d
            #define TRANSFORM_COUNT %d
            #define SPRITE_COUNT %d

            in vec3 Position;
            in vec3 Normal;
            in vec2 TexCoords;
            in int TransformOffset;
            in int SpriteOffset;

            uniform mat4 ModelViewMat;
            uniform mat4 ProjMat;

            uniform sampler2D TransformSampler;
            uniform isampler2D SpriteSampler;

            layout(std140) uniform TextureAtlas
            {
                vec4 textureAtlas[ATLAS_SIZE];
            };

            out VS_OUT {
                vec3 normal;
                vec2 texCoords;
            } vs_out;

            float nextafter(float x, float y)
            {
                int bits = floatBitsToInt(x);

                if (x == y)
                {
                    return y;
                }
                else if (x > y)
                {
                    // x needs to get smaller
                    if (x < 0)
                    {
                        bits++;
                    }
                    else
                    {
                        bits--;
                    }
                }
                else
                {
                    // x needs to get larger
                    if (x < 0)
                    {
                        bits--;
                    }
                    else
                    {
                        bits++;
                    }
                }

                return intBitsToFloat(bits);
            }

            mat4 getTransform()
            {
                ivec2 size = textureSize(TransformSampler, 0);
                int offset = (TransformOffset + gl_InstanceID) * 4;
                int y = offset / size.x;
                int x = offset %% size.x;

                return mat4(
                    texelFetchOffset(TransformSampler, ivec2(x, y), 0, ivec2(0,0)),
                    texelFetchOffset(TransformSampler, ivec2(x, y), 0, ivec2(1,0)),
                    texelFetchOffset(TransformSampler, ivec2(x, y), 0, ivec2(2,0)),
                    texelFetchOffset(TransformSampler, ivec2(x, y), 0, ivec2(3,0))
                );
            }

            int getSprite()
            {
                ivec2 size = textureSize(SpriteSampler, 0);
                int offset = SpriteOffset + gl_InstanceID + gl_VertexID - gl_BaseVertex;
                int y = offset / size.x;
                int x = offset %% size.x;

                return texelFetch(SpriteSampler, ivec2(x, y), 0).r;
            }

            void main()
            {
                mat4 transform = getTransform();
                vec4 position = vec4(Position, 1.0);
                position -= vec4(0.5, 0.5, 0.5, 0);
                position = transform * position;
                position += vec4(0.5, 0.5, 0.5, 0);

                position += vec4(0, 32, 0, 0);
                gl_Position = ProjMat * ModelViewMat * position;
                vs_out.normal = Normal;

                // Sprite bounds is the rectangle we can draw in
                int atlasIndex = getSprite();
                vec4 spriteBounds = textureAtlas[atlasIndex];
                // Shrink it a little to avoid floating point rounding errors from sampling
                spriteBounds.x = nextafter(nextafter(spriteBounds.x, spriteBounds.z), spriteBounds.z);
                spriteBounds.y = nextafter(nextafter(spriteBounds.y, spriteBounds.w), spriteBounds.w);
                spriteBounds.z = nextafter(nextafter(spriteBounds.z, spriteBounds.x), spriteBounds.x);
                spriteBounds.w = nextafter(nextafter(spriteBounds.w, spriteBounds.y), spriteBounds.y);
                // spriteSize is the size of the sprite in UV coordinates
                vec2 spriteSize = spriteBounds.zw - spriteBounds.xy;
                vs_out.texCoords = spriteBounds.xy + (spriteSize * TexCoords);
            }
            """.formatted(atlasSize, transformCount, spriteCount));
        GL32.glCompileShader(vertex);
        var compileStatus = GlStateManager.glGetShaderi(vertex, GlConst.GL_COMPILE_STATUS);
        if (compileStatus != GlConst.GL_TRUE)
        {
            LOGGER.error("Vertex shader compilation failed: {}", GL32.glGetShaderInfoLog(vertex));
            // TODO: log info here and use a better result
            throw new RuntimeException("Failed to compile shader");
        }

        var fragment = GL32.glCreateShader(GL32.GL_FRAGMENT_SHADER);
        shaders.add(fragment);
        GL32.glShaderSource(fragment, """
            #version 150

            in VS_OUT {
                vec3 normal;
                vec2 texCoords;
            } vs_out;

            uniform sampler2D BlockAtlas;

            out vec4 color;

            void main() {
                color = texture(BlockAtlas, vs_out.texCoords);
            }
            """);
        GL32.glCompileShader(fragment);
        compileStatus = GlStateManager.glGetShaderi(fragment, GlConst.GL_COMPILE_STATUS);
        if (compileStatus != GlConst.GL_TRUE)
        {
            LOGGER.error("Fragment shader compilation failed: {}", GL32.glGetShaderInfoLog(fragment));
            // TODO: log info here and use a better result
            throw new RuntimeException("Failed to compile shader");
        }

        shader = GL32.glCreateProgram();
        GL32.glAttachShader(shader, vertex);
        GL32.glAttachShader(shader, fragment);
        GL32.glLinkProgram(shader);

        final var linkStatus = GlStateManager.glGetProgrami(shader, GlConst.GL_LINK_STATUS);
        if (linkStatus != GlConst.GL_TRUE)
        {
            LOGGER.error("Program link failed: {}", GL32.glGetProgramInfoLog(shader));
            // TODO: log info here and use a better result
            throw new RuntimeException("Failed to link programs");
        }

        projectionMatrix.setLocation(Uniform.glGetUniformLocation(shader, projectionMatrix.getName()));
        modelViewMatrix.setLocation(Uniform.glGetUniformLocation(shader, modelViewMatrix.getName()));
        transformSampler.setLocation(Uniform.glGetUniformLocation(shader, transformSampler.getName()));
        spriteSampler.setLocation(Uniform.glGetUniformLocation(shader, spriteSampler.getName()));
        blockAtlas.setLocation(Uniform.glGetUniformLocation(shader, blockAtlas.getName()));

        int textureAtlas = GL32.glGetUniformBlockIndex(shader, "TextureAtlas");
        GL32.glUniformBlockBinding(shader, textureAtlas, 1);
    }


    private static final Matrix4f mvMatrix = new Matrix4f();
    private static final Matrix4f pMatrix = new Matrix4f();
    private static final Quaternionf speenQuat = new Quaternionf();
    private static BakedSection lastSection;
    public static void render(RenderLevelStageEvent e)
    {
        if (!render)
        {
            if (builtBuffers)
            {
                GL32.glDeleteProgram(shader);
                shaders.forEach(GL32::glDeleteShader);
                GL32.glDeleteVertexArrays(vertexArray);
                buffers.forEach(GL32::glDeleteBuffers);
                textures.forEach(GL32::glDeleteTextures);
                try
                {
                    lastSection.close();
                }
                catch (Exception ex)
                {
                    throw new RuntimeException(ex);
                }
                builtBuffers = false;
            }
            lastSection = null;
            return;
        }

        if (lastSection == SectionBaker.EMPTY)
        {
            return;
        }

        if (e.getStage() == Stage.AFTER_SOLID_BLOCKS)
        {
            if (!builtBuffers)
            {
                var shaper = ((ModelManager$Gander)Minecraft.getInstance().getModelManager()).gander$getBlockModelShaper();
                var index = ((ModelManager$Gander)Minecraft.getInstance().getModelManager()).gander$getAtlasIndex();

                var section = SectionBaker.bake(
                    Minecraft.getInstance().level,
                    SectionPos.of(0, 0, 0),
                    shaper,
                    index);

                lastSection = section;
                if (section == SectionBaker.EMPTY)
                {
                    return;
                }

                buildBuffers(section);
                buildShader(section);
                builtBuffers = true;
            }

            // This is mildly frustrating...
            var position = e.getCamera().getPosition();
            mvMatrix.identity();
            mvMatrix.mul(e.getModelViewMatrix());
            mvMatrix.translate((float)-position.x, (float)-position.y, (float)-position.z);
            pMatrix.set(e.getProjectionMatrix());
            speenQuat.setAngleAxis(Math.toRadians(e.getRenderTick()), 0, 1, 0);

            // TODO: combine these into a single uniform
            projectionMatrix.set(pMatrix);
            modelViewMatrix.set(mvMatrix);
            blockAtlas.set(0);
            transformSampler.set(1);
            spriteSampler.set(2);

            GL32.glUseProgram(shader);
            GL32.glBindVertexArray(vertexArray);
            projectionMatrix.upload();
            modelViewMatrix.upload();
            blockAtlas.upload();
            transformSampler.upload();
            spriteSampler.upload();
            var tex = Minecraft.getInstance()
                .getTextureManager()
                .getTexture(TextureAtlas.LOCATION_BLOCKS);

            GL32.glActiveTexture(GL32.GL_TEXTURE0);
            GL32.glBindTexture(GL32.GL_TEXTURE_2D, tex.getId());
            GL32.glActiveTexture(GL32.GL_TEXTURE1);
            GL32.glBindTexture(GL32.GL_TEXTURE_2D, transformTexture);
            GL32.glActiveTexture(GL32.GL_TEXTURE2);
            GL32.glBindTexture(GL32.GL_TEXTURE_2D, spriteTexture);

            GL32.glBindBuffer(GL32.GL_UNIFORM_BUFFER, atlasBuffer);
            GL32.glBindBufferBase(GL32.GL_UNIFORM_BUFFER, 1, atlasBuffer);
            // TODO: Use GL 3.2 calls
            GL32.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, drawBuffer);
            GL43.glMultiDrawElementsIndirect(GL32.GL_TRIANGLES, GL32.GL_UNSIGNED_BYTE, 0, drawCount, 0);
        }
    }
}
