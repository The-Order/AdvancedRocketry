package zmaster587.advancedRocketry.world.biome;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;

import java.util.LinkedList;
import java.util.List;

public class BiomeGenMoon extends Biome {

	public BiomeGenMoon(int biomeId, boolean register) {
		super(new BiomeProperties("Moon").setRainDisabled().setBaseHeight(1f).setHeightVariation(0.01f).setRainfall(0).setTemperature(0.3f));

		this.setRegistryName(new ResourceLocation("advancedrocketry:Moon"));
//		registerBiome(biomeId, "Moon", this);
		
		//cold and dry
		this.decorator.generateFalls=false;
		this.decorator.flowersPerChunk=0;
		this.decorator.grassPerChunk=0;
		this.decorator.treesPerChunk=0;
		this.decorator.mushroomsPerChunk=0;
		this.fillerBlock = this.topBlock = AdvancedRocketryBlocks.blockMoonTurf.getDefaultState();
	}
	
	@Override
	public List getSpawnableList(EnumCreatureType p_76747_1_) {
		return new LinkedList<>();
	}
	
	@Override
	public float getSpawningChance() {
		return 0f; //Nothing spawns
	}
}
