package com.simibubi.create.foundation.data;

import java.util.function.Function;
import java.util.stream.Stream;

import com.simibubi.create.Create;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import com.tterrag.registrate.util.nullness.NonNullFunction;

import net.minecraft.core.Holder;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class TagGen {

	public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> pickaxeOnly() {
		return b -> b.tag(BlockTags.MINEABLE_WITH_PICKAXE);
	}

	public static class CreateTagsProvider<T> {

		private RegistrateTagsProvider<T> provider;
		private Function<T, ResourceKey<T>> keyExtractor;

		public CreateTagsProvider(RegistrateTagsProvider<T> provider, Function<T, Holder.Reference<T>> refExtractor) {
			this.provider = provider;
			this.keyExtractor = refExtractor.andThen(Holder.Reference::key);
		}

		public CreateTagAppender<T> tag(TagKey<T> tag) {
			TagBuilder tagbuilder = getOrCreateRawBuilder(tag);
			return new CreateTagAppender<>(tagbuilder, keyExtractor, Create.ID);
		}

		public TagBuilder getOrCreateRawBuilder(TagKey<T> tag) {
			return provider.addTag(tag).getInternalBuilder();
		}

	}

	public static class CreateTagAppender<T> extends TagsProvider.TagAppender<T> {

		private Function<T, ResourceKey<T>> keyExtractor;

		public CreateTagAppender(TagBuilder pBuilder, Function<T, ResourceKey<T>> pKeyExtractor, String modId) {
			super(pBuilder, modId);
			this.keyExtractor = pKeyExtractor;
		}

		public CreateTagAppender<T> add(T entry) {
			this.add(this.keyExtractor.apply(entry));
			return this;
		}

		@SafeVarargs
		public final CreateTagAppender<T> add(T... entries) {
			Stream.<T>of(entries)
				.map(this.keyExtractor)
				.forEach(this::add);
			return this;
		}

	}
}
