package dev.compactmods.gander_test.network;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import dev.compactmods.gander.runtime.baked.section.SectionBaker;
import dev.compactmods.gander.runtime.baked.section.SectionBaker.BakedSection;
import dev.compactmods.gander.runtime.baked.section.SectionBaker.DrawCall;
import dev.compactmods.gander_test.GanderTestMod;
import dev.compactmods.gander.runtime.baked.model.ModelRebaker;
import dev.compactmods.gander.runtime.baked.texture.AtlasIndexer;
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
        key -> new GanderDebugRenderPacket(Objects.requireNonNull(BuiltInRegistries.BLOCK.get(key)).defaultBlockState()));

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
    private static int transformBuffer;
    private static int spriteBuffer;
    private static int atlasBuffer;
    private static final IntList buffers = new IntArrayList();

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

        transformBuffer = buildTransformBuffer(section);
        spriteBuffer = buildSpriteBuffer(section);
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

    private static int buildSpriteBuffer(BakedSection section)
    {
        int count = 0;
        for (var call : section.drawCalls())
        {
            count += call.textures().limit();
        }

        var spriteBuffer = GL32.glGenBuffers();
        GL32.glBindBuffer(GL32.GL_UNIFORM_BUFFER, spriteBuffer);
        GL32.glBufferData(GL32.GL_UNIFORM_BUFFER, count * 4, GL32.GL_STATIC_DRAW);
        buffers.add(spriteBuffer);

        int index = 0;
        for (var call : section.drawCalls())
        {
            try (var stack = stackPush())
            {
                var sprites = stack.mallocInt(call.textures().limit());
                sprites.put(call.textures());
                sprites.flip();
                call.textures().rewind();

                GL32.glBufferSubData(GL32.GL_UNIFORM_BUFFER, index * 4, sprites);
            }

            index += call.textures().limit();
        }

        return spriteBuffer;
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

    private static int buildTransformBuffer(BakedSection section)
    {
        int count = 0;
        for (var call : section.drawCalls())
        {
            count += call.transforms().limit();
        }

        var transformBuffer = GL32.glGenBuffers();
        GL32.glBindBuffer(GL32.GL_UNIFORM_BUFFER, transformBuffer);
        GL32.glBufferData(GL32.GL_UNIFORM_BUFFER, count * 4, GL32.GL_STATIC_DRAW);
        buffers.add(transformBuffer);

        int index = 0;
        for (var call : section.drawCalls())
        {
            try (var stack = stackPush())
            {
                var transforms = stack.mallocFloat(call.transforms().limit());
                transforms.put(call.transforms());
                transforms.flip();
                call.transforms().rewind();

                GL32.glBufferSubData(GL32.GL_UNIFORM_BUFFER, index * 4, transforms);
            }

            index += call.transforms().limit();
        }

        return transformBuffer;
    }

    private static int buildAtlasBuffer(BakedSection section)
    {
        var spriteBuffer = GL32.glGenBuffers();
        GL32.glBindBuffer(GL32.GL_UNIFORM_BUFFER, spriteBuffer);
        buffers.add(spriteBuffer);

        try (var stack = stackPush())
        {
            var atlas = stack.mallocFloat(section.atlas().limit());
            atlas.put(section.atlas());
            atlas.flip();
            section.atlas().rewind();

            GL32.glBufferData(GL32.GL_UNIFORM_BUFFER, atlas, GL32.GL_STATIC_DRAW);
        }

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

    private static final IntList shaders = new IntArrayList();

    private static void buildShader(BakedSection section)
    {
        projectionMatrix = new Uniform("ProjMat", Uniform.UT_MAT4, 16, null);
        modelViewMatrix = new Uniform("ModelViewMat", Uniform.UT_MAT4, 16, null);

        var atlasSize = section.atlas().limit() / 4;
        var transformCount = section.drawCalls().stream().mapToInt(DrawCall::transformCount).sum();
        var spriteCount = section.drawCalls().stream().mapToInt(DrawCall::textureCount).sum();

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
            
            layout(std140) uniform Transforms
            {
                mat4 transforms[TRANSFORM_COUNT];
            };
            
            layout(std140) uniform Sprites
            {
                int spriteIndices[SPRITE_COUNT];
            };

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
            
            void main() {
                mat4 transform = transforms[TransformOffset + gl_InstanceID];
                vec4 position = vec4(Position, 1.0);
                position -= vec4(0.5, 0.5, 0.5, 0);
                position = transform * position;
                position += vec4(0.5, 0.5, 0.5, 0);

                position += vec4(0, 1, 0, 0);
                gl_Position = ProjMat * ModelViewMat * position;
                vs_out.normal = Normal;

                // Sprite bounds is the rectangle we can draw in
                int atlasIndex = spriteIndices[SpriteOffset + gl_VertexID - gl_BaseVertex];
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

        int transforms = GL32.glGetUniformBlockIndex(shader, "Transforms");
        GL32.glUniformBlockBinding(shader, transforms, 0);
        int sprites = GL32.glGetUniformBlockIndex(shader, "Sprites");
        GL32.glUniformBlockBinding(shader, sprites, 1);
        int textureAtlas = GL32.glGetUniformBlockIndex(shader, "TextureAtlas");
        GL32.glUniformBlockBinding(shader, textureAtlas, 2);
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
                var rebaker = ModelRebaker.of(Minecraft.getInstance().getModelManager());
                var indexer = AtlasIndexer.of(Minecraft.getInstance().getModelManager());
                var section = SectionBaker.bake(
                    Minecraft.getInstance().level,
                    SectionPos.of(0, 0, 0),
                    rebaker,
                    indexer);

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

            GL32.glUseProgram(shader);
            GL32.glBindVertexArray(vertexArray);
            projectionMatrix.upload();
            modelViewMatrix.upload();
            var tex = Minecraft.getInstance()
                .getTextureManager()
                .getTexture(TextureAtlas.LOCATION_BLOCKS);
            GL32.glActiveTexture(GL32.GL_TEXTURE0);
            GL32.glBindTexture(GL32.GL_TEXTURE_2D, tex.getId());
            GL32.glBindBuffer(GL32.GL_UNIFORM_BUFFER, transformBuffer);
            GL32.glBindBufferBase(GL32.GL_UNIFORM_BUFFER, 0, transformBuffer);
            GL32.glBindBuffer(GL32.GL_UNIFORM_BUFFER, spriteBuffer);
            GL32.glBindBufferBase(GL32.GL_UNIFORM_BUFFER, 1, spriteBuffer);
            GL32.glBindBuffer(GL32.GL_UNIFORM_BUFFER, atlasBuffer);
            GL32.glBindBufferBase(GL32.GL_UNIFORM_BUFFER, 2, atlasBuffer);
            // TODO: Use GL 3.2 calls
            GL32.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, drawBuffer);
            GL43.glMultiDrawElementsIndirect(GL32.GL_TRIANGLES, GL32.GL_UNSIGNED_BYTE, 0, drawCount, 0);
        }
    }
}
