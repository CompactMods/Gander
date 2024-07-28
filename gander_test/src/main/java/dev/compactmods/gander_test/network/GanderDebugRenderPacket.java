package dev.compactmods.gander_test.network;

//import dev.compactmods.gander.render.baked.model.ModelRebaker;
//import dev.compactmods.gander.render.baked.section.SectionBaker;
//import dev.compactmods.gander.render.baked.texture.AtlasBaker;


/*public record GanderDebugRenderPacket(BlockState state) implements CustomPacketPayload
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
            lastBlockState = pkt.state();
        }
    }

    private static final int COUNT = (1 << 18) - 1;
    private static boolean builtBuffers = false;
    private static int vertexArray;
    private static int numberIndices;
    private static int spriteUVs;

    private static final IntList buffers = new IntArrayList();

    private static void buildBuffers(BlockState state)
    {
        var modelLocation = BlockModelShaper.stateToModelLocation(state);
        var archetypes = ModelRebaker.getArchetypeMeshes(modelLocation);
        var model = archetypes.stream().findFirst().get();
        var mesh = model.mesh();
        var textureAtlas = AtlasBaker.getAtlasBuffer(TextureAtlas.LOCATION_BLOCKS);
        var indexes = AtlasBaker.getAtlasIndexes(TextureAtlas.LOCATION_BLOCKS);
        var materialInstances = ModelRebaker.getMaterialInstances(modelLocation);

        vertexArray = GL32.glGenVertexArrays();
        GL32.glBindVertexArray(vertexArray);

        try (var stack = stackPush())
        {
            var vertices = stack.mallocFloat(mesh.vertices().limit());
            vertices.put(mesh.vertices());
            vertices.flip();

            int vertexBuffer = GL32.glGenBuffers();
            buffers.add(vertexBuffer);
            GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, vertexBuffer);
            GL32.glBufferData(GL32.GL_ARRAY_BUFFER, vertices, GL32.GL_STATIC_DRAW);
        }

        try (var stack = stackPush())
        {
            var indices = stack.malloc(mesh.indices().limit());
            indices.put(mesh.indices());
            indices.flip();

            int indexBuffer = GL32.glGenBuffers();
            buffers.add(indexBuffer);
            GL32.glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
            GL32.glBufferData(GL32.GL_ELEMENT_ARRAY_BUFFER, indices, GL32.GL_STATIC_DRAW);
            GL32.glVertexAttribPointer(0, 3, GL32.GL_FLOAT, false, 0, 0);
            GL32.glEnableVertexAttribArray(0);
            numberIndices = mesh.indices().limit();
        }

        try (var stack = stackPush())
        {
            var normals = stack.mallocFloat(mesh.normals().limit());
            normals.put(mesh.normals());
            normals.flip();

            int normalBuffer = GL32.glGenBuffers();
            buffers.add(normalBuffer);
            GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, normalBuffer);
            GL32.glBufferData(GL32.GL_ARRAY_BUFFER, normals, GL32.GL_STATIC_DRAW);
            GL32.glVertexAttribPointer(1, 3, GL32.GL_FLOAT, false, 0, 0);
            GL32.glEnableVertexAttribArray(1);
        }

        try (var stack = stackPush())
        {
            var uvs = stack.mallocFloat(mesh.uvs().limit());
            uvs.put(mesh.uvs());
            uvs.flip();

            int uvBuffer = GL32.glGenBuffers();
            buffers.add(uvBuffer);
            GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, uvBuffer);
            GL32.glBufferData(GL32.GL_ARRAY_BUFFER, uvs, GL32.GL_STATIC_DRAW);
            GL32.glVertexAttribPointer(2, 2, GL32.GL_FLOAT, false, 0, 0);
            GL32.glEnableVertexAttribArray(2);
        }

        try (var stack = stackPush())
        {
            var sprites = stack.mallocInt(mesh.vertexCount());

            for (int i = 0; i < mesh.vertexCount(); i++)
            {
                var material = mesh.materials().get(mesh.materialIndexes()[i]);
                var instances = materialInstances.get(material);
                if (instances.isEmpty())
                {
                    sprites.put(indexes.indexOf(MissingTextureAtlasSprite.getLocation()));
                }
                else
                {
                    var instance = instances.stream().findFirst().get();
                    var index = indexes.indexOf(instance.getEffectiveTexture());
                    if (index < 0)
                    {
                        sprites.put(indexes.indexOf(MissingTextureAtlasSprite.getLocation()));
                    }
                    else
                    {
                        sprites.put(index);
                    }
                }
            }
            sprites.flip();

            int spriteIndices = GL32.glGenBuffers();
            buffers.add(spriteIndices);
            GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, spriteIndices);
            GL32.glBufferData(GL32.GL_ARRAY_BUFFER, sprites, GL32.GL_STATIC_DRAW);
            GL32.glVertexAttribIPointer(3, 1, GL32.GL_INT, 0, 0);
            GL32.glEnableVertexAttribArray(3);
        }

        try (var stack = stackPush())
        {
            var textures = stack.mallocFloat(textureAtlas.limit());
            textures.put(textureAtlas);
            textures.flip();

            spriteUVs = GL32.glGenBuffers();
            buffers.add(spriteUVs);
            GL32.glBindBuffer(GL32.GL_UNIFORM_BUFFER, spriteUVs);
            GL32.glBufferData(GL32.GL_UNIFORM_BUFFER, textures, GL32.GL_STATIC_DRAW);
        }

        builtBuffers = true;
    }

    private static boolean builtShader = false;
    private static int shader;
    private static Uniform projectionMatrix;
    private static Uniform modelViewMatrix;

    private static BlockState lastBlockState;
    private static final IntList shaders = new IntArrayList();

    private static void buildShader()
    {
        projectionMatrix = new Uniform("ProjMat", Uniform.UT_MAT4, 16, null);
        modelViewMatrix = new Uniform("ModelViewMat", Uniform.UT_MAT4, 16, null);

        var atlasSize = AtlasBaker.getAtlasIndexes(TextureAtlas.LOCATION_BLOCKS).size();
        var vertex = GL32.glCreateShader(GL32.GL_VERTEX_SHADER);
        shaders.add(vertex);
        // TODO: this requires 3.3...
        GL32.glShaderSource(vertex, """
            #version 330

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
                // section relative coords are packed in XZY order 
                int x = gl_InstanceID & 0x3F;
                int z = (gl_InstanceID >> 6) & 0x3F;
                int y = (gl_InstanceID >> 12) & 0x3F;
                vec4 position = vec4(Position, 1.0);
                position -= vec4(x, y, z, 0.0f);

                gl_Position = ProjMat * ModelViewMat * position;
                vs_out.normal = Normal;

                // Sprite bounds is the rectangle we can draw in
                vec4 spriteBounds = textureAtlas[AtlasIndex];
                // Shrink it a little to avoid floating point rounding errors from sampling
                spriteBounds.x = nextafter(nextafter(spriteBounds.x, spriteBounds.z), spriteBounds.z);
                spriteBounds.y = nextafter(nextafter(spriteBounds.y, spriteBounds.w), spriteBounds.w);
                spriteBounds.z = nextafter(nextafter(spriteBounds.z, spriteBounds.x), spriteBounds.x);
                spriteBounds.w = nextafter(nextafter(spriteBounds.w, spriteBounds.y), spriteBounds.y);
                // spriteSize is the size of the sprite in UV coordinates
                vec2 spriteSize = spriteBounds.zw - spriteBounds.xy;
                vs_out.texCoords = spriteBounds.xy + (spriteSize * TexCoords);
            }
            """.formatted(atlasSize));
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

        int textureAtlas = GL32.glGetUniformBlockIndex(shader, "TextureAtlas");
        GL32.glUniformBlockBinding(shader, textureAtlas, 0);

        builtShader = true;
    }


    private static final Matrix4f mvMatrix = new Matrix4f();
    private static final Matrix4f pMatrix = new Matrix4f();
    public static void render(RenderLevelStageEvent e)
    {
        if (!render)
        {
            if (builtShader)
            {
                GL32.glDeleteProgram(shader);
                shaders.forEach(GL32::glDeleteShader);
                builtShader = false;
            }
            if (builtBuffers)
            {
                GL32.glDeleteVertexArrays(vertexArray);
                buffers.forEach(GL32::glDeleteBuffers);
                builtBuffers = false;
            }
            return;
        }

        if (e.getStage() == Stage.AFTER_SOLID_BLOCKS)
        {
            if (!builtBuffers)
            {
                buildBuffers(lastBlockState);
                SectionBaker.bake(Minecraft.getInstance().level, SectionPos.of(0,0,0));
            }
            if (!builtShader) buildShader();

            // This is mildly frustrating...
            var position = e.getCamera().getPosition();
            mvMatrix.identity();
            mvMatrix.mul(e.getModelViewMatrix());
            mvMatrix.translate((float)-position.x, (float)-position.y, (float)-position.z);
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
            GL32.glBindBuffer(GL32.GL_UNIFORM_BUFFER, spriteUVs);
            GL32.glBindBufferBase(GL32.GL_UNIFORM_BUFFER, 0, spriteUVs);
            GL32.glDrawElementsInstanced(GL32.GL_TRIANGLES, numberIndices, GL32.GL_UNSIGNED_BYTE, 0, (1 << 18) - 1);
        }
    }


}
*/