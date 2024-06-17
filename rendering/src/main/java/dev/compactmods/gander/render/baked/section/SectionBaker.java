package dev.compactmods.gander.render.baked.section;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceLinkedOpenHashMap;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.SectionPos;
import net.minecraft.world.item.component.BundleContents.Mutable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import net.minecraft.world.level.block.Block;

import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.phys.shapes.Shapes;
import org.roaringbitmap.RoaringBitmap;
import org.roaringbitmap.buffer.ImmutableRoaringBitmap;
import org.roaringbitmap.buffer.MutableRoaringBitmap;

import java.util.EnumMap;
import java.util.Map;
import java.util.SortedMap;

public final class SectionBaker
{
    private enum BlockMetadata
    {
        SupportsOcclusion,
        OccupiesFullBlock,
        HasBlockEntity,
        HasFluidState,
        HasOffset,
        HasCustomRender,
        HasAmbientOcclusion;

        public void set(Map<BlockMetadata, RoaringBitmap> map, BlockPos position)
        {
            map.computeIfAbsent(this, unused -> new RoaringBitmap())
                .add(SectionPos.sectionRelativePos(position));
        }

        public boolean get(Map<BlockMetadata, RoaringBitmap> map, BlockPos position)
        {
            var bitset = map.get(this);
            if (bitset == null) return false;

            return bitset.contains(SectionPos.sectionRelativePos(position));
        }
    }

    private SectionBaker() { }

    public static SectionMesh bake(
        Level level,
        SectionPos section,
        BlockModelShaper modelShaper)
    {
        var chunk = level.getChunk(section.x(), section.z());
        // If this returns true, we're likely outside of the map.
        if (chunk.isEmpty()) return SectionMesh.EMPTY;

        // A bitset of all non-air blocks.
        var presenceMap = new RoaringBitmap();
        // The metadata map of every block in this section.
        var stateMetadata = new EnumMap<BlockMetadata, RoaringBitmap>(BlockMetadata.class);
        // The occlusion map of every block face in this section.
        // A bit set in this map means that the face CAN be occluded.
        var faceOcclusion = new EnumMap<Direction, RoaringBitmap>(Direction.class);
        // The visibility map of every block face in this section.
        // A bit set in this map means that the face IS rendered.
        var faceVisibility = new EnumMap<Direction, RoaringBitmap>(Direction.class);
        // The mesh map of every block in this section.
        var meshMap = new Object2ReferenceLinkedOpenHashMap<BakedModel, RoaringBitmap>();

        section.blocksInside()
            .forEach(pos -> {
                // TODO: treat fluid layer separately?
                var state = chunk.getBlockState(pos);

                // Air blocks don't have anything special about them (currently)
                if (state.isAir())
                    return;

                presenceMap.add(SectionPos.sectionRelativePos(pos));

                computeStateFlags(chunk, state, pos, modelShaper, stateMetadata);
                //computeOcclusion(chunk, state, pos, stateMetadata, faceOcclusion);

                var mesh = modelShaper.getBlockModel(state);
                var set = meshMap.getAndMoveToFirst(mesh);
                if (set == null)
                {
                    set = new RoaringBitmap();
                    meshMap.putAndMoveToFirst(mesh, set);
                }

                set.add(SectionPos.sectionRelativePos(pos));
            });

        // TODO: :)
        if (stateMetadata.isEmpty()) return SectionMesh.EMPTY;

        //for (var axis : Axis.values())
        //    computeVisibility(axis, presenceMap, faceOcclusion, faceVisibility);

        computeMesh(stateMetadata, faceVisibility, meshMap);
        return null;
    }

    private static void computeStateFlags(
        BlockGetter blocks,
        BlockState state,
        BlockPos position,
        BlockModelShaper modelShaper,
        Map<SectionBaker.BlockMetadata, RoaringBitmap> metadataBitmap)
    {
        if (state.canOcclude())
            BlockMetadata.SupportsOcclusion.set(metadataBitmap, position);

        if (Block.isShapeFullBlock(state.getOcclusionShape(blocks, position)))
            BlockMetadata.OccupiesFullBlock.set(metadataBitmap, position);

        if (state.hasBlockEntity())
            BlockMetadata.HasBlockEntity.set(metadataBitmap, position);

        var fluidState = blocks.getFluidState(position);
        if (!fluidState.isEmpty())
            BlockMetadata.HasFluidState.set(metadataBitmap, position);

        if (state.hasOffsetFunction())
            BlockMetadata.HasOffset.set(metadataBitmap, position);

        /*
         * TODO: there seems to be an edge case in the vanilla/NeoForge code:
         *  if BlockState#getRenderShape() returns ENTITYBLOCK_ANIMATED then
         *  SectionRenderDispatcher will attempt to render the block, however
         *  BlockRenderDispatcher#renderBatched() will only render if the
         *  returned shape is MODEL, meaning nothing will get rendered.
         *  The assumption is thus:
         *   - INVISIBLE means no rendering at all.
         *   - ENTITYBLOCK_ANIMATED means delegate to BlockEntityRenderer?
         *   - MODEL means a standard json model
         *   - MODEL w/ isCustomRenderer means we're an item?
         */
        switch (state.getRenderShape())
        {
            case INVISIBLE:
                return;
            case ENTITYBLOCK_ANIMATED:
                BlockMetadata.HasCustomRender.set(metadataBitmap, position);
                break;
            case MODEL:
                var model = modelShaper.getBlockModel(state);
                if (model.isCustomRenderer())
                    throw new IllegalStateException(
                        "BlockState " + state + " has renders as a model " +
                            "but has a custom renderer");

                if (model.useAmbientOcclusion())
                    BlockMetadata.HasAmbientOcclusion.set(metadataBitmap, position);
                break;
        }
    }

    private static void computeOcclusion(
        BlockGetter blocks,
        BlockState state,
        BlockPos position,
        Map<BlockMetadata, RoaringBitmap> metadata,
        Map<Direction, RoaringBitmap> occlusionMap)
    {
        for (var direction : Direction.values())
        {
            var adjacent = position.relative(direction);
            // If we're on the section boundary, this will return -4 or 4
            // Theory: popcnt(1111) - popcnt(0000) = 4
            // Theory: popcnt(0000) - popcnt(1111) = -4
            // This applies in all directions as they only change one axis.
            var bitDiff = Integer.bitCount(SectionPos.sectionRelativePos(position))
                - Integer.bitCount(SectionPos.sectionRelativePos(adjacent));

            // We assume that blocks outside of this section can never occlude.
            // This is because external occlusions are handled elsewhere.
            if (bitDiff == 4 || bitDiff == -4)
            {
                continue;
            }

            // If both blocks are "solid", we assume they can occlude.
            if (BlockMetadata.SupportsOcclusion.get(metadata, position)
                && BlockMetadata.OccupiesFullBlock.get(metadata, position)
                && BlockMetadata.SupportsOcclusion.get(metadata, adjacent)
                && BlockMetadata.OccupiesFullBlock.get(metadata, adjacent))
            {
                addOcclusion(occlusionMap, direction, position);
                continue;
            }

            // If we're marked as skipping rendering, we can occlude.
            var adjacentState = blocks.getBlockState(adjacent);
            if (state.skipRendering(adjacentState, direction))
            {
                addOcclusion(occlusionMap, direction, position);
                continue;
            }

            // If they're marked as hiding us, we can occlude.
            if (state.supportsExternalFaceHiding()
                && adjacentState.hidesNeighborFace(blocks, adjacent, state, direction.getOpposite()))
            {
                addOcclusion(occlusionMap, direction, position);
                continue;
            }

            // Otherwise, we need to do a potentially expensive computation.
            var face = state.getFaceOcclusionShape(blocks, position, direction);
            var other = blocks.getBlockState(adjacent)
                .getFaceOcclusionShape(blocks, adjacent, direction.getOpposite());

            if (Shapes.faceShapeOccludes(face, other))
            {
                addOcclusion(occlusionMap, direction, position);
            }
        }
    }

    private static void addOcclusion(
        Map<Direction, RoaringBitmap> occlusionMap,
        Direction direction,
        BlockPos position)
    {
        occlusionMap.computeIfAbsent(direction, unused -> new RoaringBitmap())
            .add(SectionPos.sectionRelativePos(position));
    }

    private static final short OneX = sectionRelativePos(1, 0, 0);
    private static final short OneY = sectionRelativePos(0, 1, 0);
    private static final short OneZ = sectionRelativePos(0, 0, 1);
    private static final short MaxPos = sectionRelativePos(15, 15, 15);
    private static void computeVisibility(
        Axis axis,
        RoaringBitmap presenceMap,
        Map<Direction, RoaringBitmap> occlusionMap,
        Map<Direction, RoaringBitmap> visibilityMap)
    {
        // e.g. DOWN for Axis = Y
        var negativeDir = Direction.fromAxisAndDirection(axis, AxisDirection.NEGATIVE);
        // e.g. UP for Axis = Y
        var positiveDir = Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE);

        // e.g. all of the positions where we can occlude DOWN for Axis = Y
        var negativeFace = occlusionMap.get(negativeDir);
        // e.g. all of the positions where we can occlude UP for Axis = Y
        var positiveFace = occlusionMap.get(positiveDir);

        // All of the +ve positions where we render +ve and -ve
        var positiveResult = negativeFace.clone();
        positiveResult.andNot(getMinPositions(axis));
        positiveResult = RoaringBitmap.addOffset(positiveResult, -axis.choose(OneX, OneY, OneZ));
        positiveResult.and(positiveFace);
        positiveResult.flip(0L, MaxPos);
        positiveResult.and(presenceMap);

        // All of the -ve positions where we render -ve and +ve
        var negativeResult = positiveFace.clone();
        negativeResult.andNot(getMaxPositions(axis));
        negativeResult = RoaringBitmap.addOffset(negativeResult, axis.choose(OneX, OneY, OneZ));
        negativeResult.and(negativeFace);
        negativeResult.flip(0L, MaxPos);
        negativeResult.and(presenceMap);

        visibilityMap.put(negativeDir, negativeResult);
        visibilityMap.put(positiveDir, positiveResult);
    }

    // TODO: cache this?
    private static RoaringBitmap getMinPositions(Axis axis)
    {
        var result = new RoaringBitmap();

        for (int otherAxis1 = 0; otherAxis1 < 16; otherAxis1++)
        {
            for (int otherAxis2 = 0; otherAxis2 < 16; otherAxis2++)
            {
                result.add(switch (axis) {
                    case X -> sectionRelativePos(0, otherAxis1, otherAxis2);
                    case Y -> sectionRelativePos(otherAxis1, 0, otherAxis2);
                    case Z -> sectionRelativePos(otherAxis1, otherAxis2, 0);
                });
            }
        }

        return result;
    }

    // TODO: cache this?
    private static RoaringBitmap getMaxPositions(Axis axis)
    {
        var result = new RoaringBitmap();

        for (int otherAxis1 = 0; otherAxis1 < 16; otherAxis1++)
        {
            for (int otherAxis2 = 0; otherAxis2 < 16; otherAxis2++)
            {
                result.add(switch (axis) {
                    case X -> sectionRelativePos(15, otherAxis1, otherAxis2);
                    case Y -> sectionRelativePos(otherAxis1, 15, otherAxis2);
                    case Z -> sectionRelativePos(otherAxis1, otherAxis2, 15);
                });
            }
        }

        return result;
    }

    // SectionPos only has an overload of this accepting BlockPos :(
    private static short sectionRelativePos(int x, int y, int z)
    {
        var relX = SectionPos.sectionRelative(x);
        var relY = SectionPos.sectionRelative(y);
        var relZ = SectionPos.sectionRelative(z);

        return (short)(relX << 8 | relZ << 4 | relY);
    }

    private static void computeMesh(
        Map<BlockMetadata, RoaringBitmap> metadataMap,
        Map<Direction, RoaringBitmap> facesToRender,
        SortedMap<BakedModel, RoaringBitmap> meshIndexes)
    {

    }
}
