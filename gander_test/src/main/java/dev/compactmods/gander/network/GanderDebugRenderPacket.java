package dev.compactmods.gander.network;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import dev.compactmods.gander.GanderTestMod;
import dev.compactmods.gander.render.baked.model.ModelRebaker;
import dev.compactmods.gander.render.baked.texture.AtlasBaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.client.util.DebuggingHelper;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Matrix4f;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.system.MemoryStack.stackPush;

public record GanderDebugRenderPacket() implements CustomPacketPayload
{
    private static boolean render = false;

    public static final GanderDebugRenderPacket INSTANCE = new GanderDebugRenderPacket();

    public static final Type<GanderDebugRenderPacket> ID = new Type<>(GanderTestMod.asResource("debug"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GanderDebugRenderPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

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
        }
    }

    private static final int COUNT = (1 << 18) - 1;
    private static final ModelResourceLocation modelRL = ModelResourceLocation.vanilla("stone", "");
    private static boolean builtBuffers = false;
    private static int vertexArray;
    private static int numberIndices;
    private static int spriteUVs;

    private static void buildBuffers()
    {
        var archetypes = ModelRebaker.getModelArchetypes(modelRL);
        var models = ModelRebaker.getArchetypeMeshes(archetypes.stream().findFirst().get());
        var model = ModelRebaker.getMesh(models.stream().findFirst().get());
        var textures = AtlasBaker.getAtlasBuffer(TextureAtlas.LOCATION_BLOCKS);
        var indexes = AtlasBaker.getAtlasIndexes(TextureAtlas.LOCATION_BLOCKS);
        var stoneIndex = indexes.indexOf(new ResourceLocation("minecraft:block/stone"));

        vertexArray = GL32.glGenVertexArrays();
        GL32.glBindVertexArray(vertexArray);

        try (var stack = stackPush())
        {
            var vertices = stack.mallocFloat(model.vertices().remaining());
            var indices = stack.malloc(model.indices().remaining());
            var normals = stack.mallocFloat(model.normals().remaining());
            var uvs = stack.mallocFloat(model.uvs().remaining());
            var textureUvs = stack.mallocFloat(textures.remaining());
            var textureIndices = MemoryUtil.memAllocInt(COUNT);

            vertices.put(model.vertices());
            vertices.flip();
            indices.put(model.indices());
            indices.flip();
            normals.put(model.normals());
            normals.flip();
            uvs.put(model.uvs());
            uvs.flip();
            textureUvs.put(textures);
            textureUvs.flip();

            for (int i = 0; i < COUNT; i++)
            {
                textureIndices.put(stoneIndex);
            }
            textureIndices.flip();

            int vertexBuffer = GL32.glGenBuffers();
            GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vertexBuffer);
            GL32.glBufferData(GL32.GL_ARRAY_BUFFER, vertices, GL32.GL_STATIC_DRAW);

            int indexBuffer = GL32.glGenBuffers();
            GL32.glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
            GL32.glBufferData(GL32.GL_ELEMENT_ARRAY_BUFFER, indices, GL32.GL_STATIC_DRAW);
            GL32.glVertexAttribPointer(0, 3, GL32.GL_FLOAT, false, 0, 0);
            GL32.glEnableVertexAttribArray(0);
            numberIndices = model.indices().limit();

            int normalBuffer = GL32.glGenBuffers();
            GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, normalBuffer);
            GL32.glBufferData(GL32.GL_ARRAY_BUFFER, normals, GL32.GL_STATIC_DRAW);
            GL32.glVertexAttribPointer(1, 3, GL32.GL_FLOAT, false, 0, 0);
            GL32.glEnableVertexAttribArray(1);

            int uvBuffer = GL32.glGenBuffers();
            GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, uvBuffer);
            GL32.glBufferData(GL32.GL_ARRAY_BUFFER, uvs, GL32.GL_STATIC_DRAW);
            GL32.glVertexAttribPointer(2, 2, GL32.GL_FLOAT, false, 0, 0);
            GL32.glEnableVertexAttribArray(2);

            int spriteIndices = GL32.glGenBuffers();
            GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, spriteIndices);
            GL32.glBufferData(GL32.GL_ARRAY_BUFFER, textureIndices, GL32.GL_STATIC_DRAW);
            GL32.glVertexAttribIPointer(3, 1, GL32.GL_INT, 0, 0);
            if (GL.getCapabilities().OpenGL33)
            {
                GL33.glVertexAttribDivisor(3, 1);
            }
            else if (GL.getCapabilities().GL_ARB_instanced_arrays)
            {
                ARBInstancedArrays.glVertexAttribDivisorARB(3, 1);
            }
            GL32.glEnableVertexAttribArray(3);

            spriteUVs = GL32.glGenBuffers();
            GL32.glBindBuffer(GL32.GL_UNIFORM_BUFFER, spriteUVs);
            GL32.glBufferData(GL32.GL_UNIFORM_BUFFER, textureUvs, GL32.GL_STATIC_DRAW);
        }

        builtBuffers = true;
    }

    private static boolean builtShader = false;
    private static int shader;
    private static Uniform projectionMatrix;
    private static Uniform modelViewMatrix;

    private static void buildShader()
    {
        projectionMatrix = new Uniform("ProjMat", Uniform.UT_MAT4, 16, null);
        modelViewMatrix = new Uniform("ModelViewMat", Uniform.UT_MAT4, 16, null);

        var atlasSize = AtlasBaker.getAtlasIndexes(TextureAtlas.LOCATION_BLOCKS).size();
        var vertex = GL32.glCreateShader(GL32.GL_VERTEX_SHADER);
        GL32.glShaderSource(vertex, """
            #version 150

            in vec3 Position;
            in vec3 Normal;
            in vec2 TexCoords;
            in int AtlasIndex;

            // TODO: these should be a single UBO
            uniform mat4 ModelViewMat;
            uniform mat4 ProjMat;

            layout(std140) uniform TextureAtlas
            {
                vec4 textureAtlas[%d];
            };

            out VS_OUT {
                vec3 normal;
                vec2 texCoords;
            } vs_out;
            
            void main() {
                // section relative coords are packed in XZY order 
                int x = gl_InstanceID & 0x3F;
                int z = (gl_InstanceID >> 6) & 0x3F;
                int y = (gl_InstanceID >> 12) & 0x3F;
                vec4 position = vec4(Position, 1.0);
                // Models are scaled 16x
                position -= vec4(x * 16.0f, y * 16.0f, z * 16.0f, 0.0f);

                gl_Position = ProjMat * ModelViewMat * position;
                vs_out.normal = Normal;

                // Sprite bounds is the rectangle we can draw in
                vec4 spriteBounds = textureAtlas[AtlasIndex];
                // uv is the 0..1 as a percentage of spriteBounds
                vec2 uv = TexCoords / 16.0f;
                // spriteSize is the size of the sprite in UV coordinates
                vec2 spriteSize = spriteBounds.zw - spriteBounds.xy;
                vs_out.texCoords = spriteBounds.xy + (spriteSize * uv);
            }
            """.formatted(atlasSize));
        GL32.glCompileShader(vertex);
        var compileStatus = GlStateManager.glGetShaderi(vertex, GlConst.GL_COMPILE_STATUS);
        if (compileStatus != GlConst.GL_TRUE)
        {
            // TODO: log info here and use a better result
            throw new RuntimeException("Failed to compile shader");
        }

        var fragment = GL32.glCreateShader(GL32.GL_FRAGMENT_SHADER);
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
            // TODO: log info here and use a better result
            throw new RuntimeException("Failed to link programs");
        }

        projectionMatrix.setLocation(Uniform.glGetUniformLocation(shader, projectionMatrix.getName()));
        modelViewMatrix.setLocation(Uniform.glGetUniformLocation(shader, modelViewMatrix.getName()));

        int textureAtlas = GL32.glGetUniformBlockIndex(shader, "TextureAtlas");
        GL32.glUniformBlockBinding(shader, textureAtlas, 0);

        builtShader = true;
    }


    private static final Matrix4f mvMatrix = new Matrix4f();
    private static final Matrix4f pMatrix = new Matrix4f();
    public static void render(RenderLevelStageEvent e)
    {
        if (!render) return;

        if (e.getStage() == Stage.AFTER_SOLID_BLOCKS)
        {
            //DebuggingHelper.releaseMouse();

            if (!builtBuffers) buildBuffers();
            if (!builtShader) buildShader();

            // This is mildly frustrating...
            var position = e.getCamera().getPosition();
            mvMatrix.identity();
            mvMatrix.mul(e.getModelViewMatrix());
            mvMatrix.translate((float)-position.x, (float)-position.y, (float)-position.z);
            mvMatrix.scale(1f/16f);
            pMatrix.set(e.getProjectionMatrix());

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
            GL32.glBindBufferBase(GL32.GL_UNIFORM_BUFFER, 0, spriteUVs);
            GL32.glDrawElementsInstanced(GL32.GL_TRIANGLES, numberIndices, GL32.GL_UNSIGNED_BYTE, 0, COUNT);
        }
    }


}
