package dev.compactmods.gander.render.baked.model;

import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;

import java.util.List;
import java.util.stream.Stream;

public final class DisplayableMeshGroup
{
    public enum Mode
    {
        All,
        Weighted
    }

    private final boolean _nested;
    private final Mode _mode;
    private final int _weight;
    private final List<WeightedEntry.Wrapper<DisplayableMesh>> _meshes;
    private final List<WeightedEntry.Wrapper<DisplayableMeshGroup>> _groups;

    private DisplayableMeshGroup(Mode mode, int weight, List<DisplayableMesh> meshes)
    {
        _nested = false;
        _mode = mode;
        _weight = weight;
        _meshes = meshes.stream()
            .map(it -> WeightedEntry.wrap(it, it.weight()))
            .toList();
        _groups = null;
    }

    // N.B. the boolean parameter is needed to make the overloads distinct.
    private DisplayableMeshGroup(Mode mode, int weight, List<DisplayableMeshGroup> groups, boolean group)
    {
        _nested = true;
        _mode = mode;
        _weight = weight;
        _groups = groups.stream()
            .map(it -> WeightedEntry.wrap(it, 1))
            .toList();
        _meshes = null;
    }

    public static DisplayableMeshGroup of()
    {
        return new DisplayableMeshGroup(Mode.All, 1, List.of());
    }

    public static DisplayableMeshGroup of(DisplayableMesh unit)
    {
        return new DisplayableMeshGroup(Mode.All, 1, List.of(unit));
    }

    public static DisplayableMeshGroup ofMeshes(Mode mode, int weight, List<DisplayableMesh> meshes)
    {
        return new DisplayableMeshGroup(mode, weight, meshes);
    }

    public static DisplayableMeshGroup ofGroups(Mode mode, int weight, List<DisplayableMeshGroup> groups)
    {
        return new DisplayableMeshGroup(mode, weight, groups, true);
    }

    public int weight()
    {
        return _weight;
    }

    /**
     * Gets a {@link Stream} of all meshes in this group and its descendents,
     * ignoring any specified {@link Mode}.
     * @return All possible {@link DisplayableMesh displayable meshes} that
     * could be displayed.
     */
    public Stream<DisplayableMesh> allMeshes()
    {
        if (_nested)
        {
            assert _groups != null;
            return _groups.stream()
                .map(WeightedEntry.Wrapper::data)
                .flatMap(DisplayableMeshGroup::allMeshes);
        }

        assert _meshes != null;
        return _meshes.stream().map(WeightedEntry.Wrapper::data);
    }

    /**
     * Gets a {@link Stream} of all meshes in this group and its descendents,
     * suitable for rendering. This method respects the {@link Mode} of any mesh
     * group.
     * @param randomSource The {@link RandomSource} to use for random weighted
     * selection.
     * @return The {@link DisplayableMesh displayable meshes} which should be
     * rendered for this mesh group.
     */
    public Stream<DisplayableMesh> meshes(RandomSource randomSource)
    {
        return _nested
            ? getNestedMeshes(randomSource)
            : getOurMeshes(randomSource);
    }

    private Stream<DisplayableMesh> getNestedMeshes(RandomSource randomSource)
    {
        assert _groups != null;
        if (_mode == Mode.All)
            return _groups.stream().flatMap(it -> it.data().meshes(randomSource));

        return WeightedRandom.getRandomItem(randomSource, _groups)
            .stream()
            .map(WeightedEntry.Wrapper::data)
            .flatMap(it -> it.meshes(randomSource));
    }

    private Stream<DisplayableMesh> getOurMeshes(RandomSource randomSource)
    {
        assert _meshes != null;
        if (_mode == Mode.All)
            return _meshes.stream().map(WeightedEntry.Wrapper::data);

        return WeightedRandom.getRandomItem(randomSource, _meshes)
            .stream()
            .map(WeightedEntry.Wrapper::data);
    }
}
