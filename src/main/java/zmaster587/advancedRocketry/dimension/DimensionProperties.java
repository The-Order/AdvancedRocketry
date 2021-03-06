package zmaster587.advancedRocketry.dimension;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.TempCategory;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.BiomeManager.BiomeEntry;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import org.apache.commons.lang3.ArrayUtils;
import scala.util.Random;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.AdvancedRocketryBiomes;
import zmaster587.advancedRocketry.api.Configuration;
import zmaster587.advancedRocketry.api.IAtmosphere;
import zmaster587.advancedRocketry.api.SatelliteRegistry;
import zmaster587.advancedRocketry.api.atmosphere.AtmosphereRegister;
import zmaster587.advancedRocketry.api.dimension.IDimensionProperties;
import zmaster587.advancedRocketry.api.dimension.solar.StellarBody;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.network.PacketDimInfo;
import zmaster587.advancedRocketry.network.PacketSatellite;
import zmaster587.advancedRocketry.stations.SpaceObjectManager;
import zmaster587.advancedRocketry.util.OreGenProperties;
import zmaster587.advancedRocketry.world.ChunkManagerPlanet;
import zmaster587.advancedRocketry.world.provider.WorldProviderPlanet;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;
import zmaster587.libVulpes.util.VulpineMath;
import zmaster587.libVulpes.util.ZUtils;

import java.util.*;
import java.util.Map.Entry;

public class DimensionProperties implements Cloneable, IDimensionProperties {

	/**
	 * Contains standardized temperature ranges for planets
	 * where 100 is earthlike, larger values are hotter
	 */
	public static enum Temps {
		TOOHOT(150),
		HOT(125),
		NORMAL(75),
		COLD(50),
		FRIGID(25),
		SNOWBALL(0);

		private int temp;
		Temps(int i) {
			temp = i;
		}

		@Deprecated
		public int getTemp() {
			return temp;
		}

		/**
		 * @param lowerBound lower Bound (inclusive)
		 * @param upperBound upper Bound (inclusive)
		 * @return true if this resides between the to bounds
		 */
		public boolean isInRange(Temps lowerBound, Temps upperBound) {
			return this.compareTo(lowerBound) <= 0 && this.compareTo(upperBound) >= 0;
		}

		/**
		 * @return a temperature that refers to the supplied value
		 */

		public static Temps getTempFromValue(int value) {
			for(Temps type : Temps.values()) {
				if(value > type.temp)
					return type;
			}
			return SNOWBALL;
		}
	}

	/**
	 * Contains standardized pressure ranges for planets
	 * where 100 is earthlike, largers values are higher pressure
	 */
	public static enum AtmosphereTypes {
		HIGHPRESSURE(125),
		NORMAL(75),
		LOW(25),
		NONE(0);

		private int value;

		private AtmosphereTypes(int value) {
			this.value = value;
		}

		public int getAtmosphereValue() {
			return value;
		}

		public static AtmosphereTypes getAtmosphereTypeFromValue(int value) {
			for(AtmosphereTypes type : AtmosphereTypes.values()) {
				if(value > type.value)
					return type;
			}
			return NONE;
		}
	}

	/**
	 * Contains default graphic {@link ResourceLocation} to display for different planet types
	 *
	 */
	public static final ResourceLocation atmosphere = new ResourceLocation("advancedrocketry:textures/planets/Atmosphere2.png");
	public static final ResourceLocation atmosphereLEO = new ResourceLocation("advancedrocketry:textures/planets/AtmosphereLEO.png");
	public static final ResourceLocation atmGlow = new ResourceLocation("advancedrocketry:textures/planets/atmGlow.png");
	public static final ResourceLocation planetRings = new ResourceLocation("advancedrocketry:textures/planets/rings.png");
	public static final ResourceLocation planetRingShadow = new ResourceLocation("advancedrocketry:textures/planets/ringShadow.png");

	public static final ResourceLocation shadow = new ResourceLocation("advancedrocketry:textures/planets/shadow.png");
	public static final ResourceLocation shadow3 = new ResourceLocation("advancedrocketry:textures/planets/shadow3.png");

	public static enum PlanetIcons {
		EARTHLIKE(new ResourceLocation("advancedrocketry:textures/planets/Earthlike.png")),
		LAVA(new ResourceLocation("advancedrocketry:textures/planets/Lava.png")),
		MARSLIKE(new ResourceLocation("advancedrocketry:textures/planets/marslike.png")),
		MOON(new ResourceLocation("advancedrocketry:textures/planets/moon.png")),
		WATERWORLD(new ResourceLocation("advancedrocketry:textures/planets/WaterWorld.png")),
		ICEWORLD(new ResourceLocation("advancedrocketry:textures/planets/IceWorld.png")),
		GASGIANTBLUE(new ResourceLocation("advancedrocketry:textures/planets/GasGiantBlue.png")),
		GASGIANTRED(new ResourceLocation("advancedrocketry:textures/planets/GasGiantOrange.png")),
		UNKNOWN(new ResourceLocation("advancedrocketry:textures/planets/Unknown.png"))
		;



		private ResourceLocation resource;
		private ResourceLocation resourceLEO;

		private PlanetIcons(ResourceLocation resource) {
			this.resource = resource;

			this.resourceLEO = new ResourceLocation(resource.toString().substring(0, resource.toString().length() - 4) + "LEO.jpg");
		}

		private PlanetIcons(ResourceLocation resource, ResourceLocation leo) {
			this.resource = resource;

			this.resourceLEO = atmosphereLEO;
		}

		public ResourceLocation getResource() {
			return resource;
		}

		public ResourceLocation getResourceLEO() {
			return resourceLEO;
		}
	}

	public static final int MAX_ATM_PRESSURE = 200;
	public static final int MIN_ATM_PRESSURE = 0;

	public static final int MAX_DISTANCE = 200;
	public static final int MIN_DISTANCE = 0;

	public static final int MAX_GRAVITY = 200;
	public static final int MIN_GRAVITY = 0;



	//True if dimension is managed and created by AR (false otherwise)
	public boolean isNativeDimension;
	//Gas giants DO NOT need a dimension registered to them
	public float[] skyColor;
	public float[] fogColor;
	public float[] ringColor;
	public float gravitationalMultiplier;
	public int orbitalDist;
	private int originalAtmosphereDensity;
	private int atmosphereDensity;
	public int averageTemperature;
	public int rotationalPeriod;
	//Stored in radians
	public double orbitTheta;
	public double prevOrbitalTheta;
	public double orbitalPhi;
	public double rotationalPhi;
	public OreGenProperties oreProperties = null;
	public String customIcon;

	StellarBody star;
	int starId;
	private String name;
	public float[] sunriseSunsetColors;
	//public ExtendedBiomeProperties biomeProperties;
	private LinkedList<BiomeEntry> allowedBiomes;
	private LinkedList<BiomeEntry> terraformedBiomes;
	private boolean isRegistered = false;
	private boolean isTerraformed = false;
	public boolean hasRings = false;
	public List<ItemStack> requiredArtifacts;

	//Planet Heirachy
	private HashSet<Integer> childPlanets;
	private int parentPlanet;
	private int planetId;
	private boolean isStation;
	private boolean isGasGiant;

	//Satallites
	private HashMap<Long,SatelliteBase> satallites;
	private HashMap<Long,SatelliteBase> tickingSatallites;
	private List<Fluid> harvestableAtmosphere;
	private HashSet<HashedBlockPosition> beaconLocations;
	private IBlockState oceanBlock;
	private int sealevel;

	public DimensionProperties(int id) {
		name = "Temp";
		resetProperties();

		planetId = id;
		parentPlanet = -1;
		childPlanets = new HashSet<Integer>();
		orbitalPhi = 0;
		ringColor = new float[] {.4f, .4f, .7f};
		oceanBlock = null;

		allowedBiomes = new LinkedList<BiomeManager.BiomeEntry>();
		terraformedBiomes = new LinkedList<BiomeManager.BiomeEntry>();
		satallites = new HashMap<>();
		requiredArtifacts = new LinkedList<ItemStack>();
		tickingSatallites = new HashMap<Long,SatelliteBase>();
		isNativeDimension = true;
		isGasGiant = false;
		hasRings = false;
		customIcon = "";
		harvestableAtmosphere = new LinkedList<Fluid>();
		beaconLocations = new HashSet<HashedBlockPosition>();
		sealevel = 63;
	}

	public DimensionProperties(int id ,String name) {
		this(id);
		this.name = name;
	}

	public DimensionProperties(int id, boolean shouldRegister) {
		this(id);
		isStation = !shouldRegister;
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch(CloneNotSupportedException e) {
			return null;
		}
	}

	/**
	 * @param world
	 * @return null to use default world gen properties, otherwise a list of ores to generate
	 */
	public OreGenProperties getOreGenProperties(World world) {
		if(oreProperties != null)
			return oreProperties;
		return OreGenProperties.getOresForPressure(AtmosphereTypes.getAtmosphereTypeFromValue(originalAtmosphereDensity), Temps.getTempFromValue(averageTemperature));
	}

	/**
	 * Resets all properties to default
	 */
	public void resetProperties() {
		fogColor = new float[] {1f,1f,1f};
		skyColor = new float[] {1f,1f,1f};
		sunriseSunsetColors = new float[] {.7f,.2f,.2f,1};
		ringColor = new float[] {.4f, .4f, .7f};
		gravitationalMultiplier = 1;
		rotationalPeriod = 24000;
		orbitalDist = 100;
		originalAtmosphereDensity = atmosphereDensity = 100;
		childPlanets = new HashSet<Integer>();
		requiredArtifacts = new LinkedList<ItemStack>();
		parentPlanet = -1;
		starId = 0;
		averageTemperature = 100;
		hasRings = false;
		harvestableAtmosphere = new LinkedList<Fluid>();
		beaconLocations = new HashSet<HashedBlockPosition>();
		sealevel = 63;
		oceanBlock = null;
	}

	public List<Fluid> getHarvestableGasses() {
		return harvestableAtmosphere;
	}

	public List<ItemStack> getRequiredArtifacts() {
		return requiredArtifacts;
	}

	@Override
	public float getGravitationalMultiplier() {
		return gravitationalMultiplier;
	}

	@Override
	public void setGravitationalMultiplier(float mult) {
		gravitationalMultiplier = mult;
	}

	/**
	 * @return the color of the sun as an array of floats represented as  {r,g,b}
	 */
	public float[] getSunColor() {
		return getStar().getColor();
	}

	/**
	 * Sets the host star for the planet
	 * @param star the star to set as the host for this planet
	 */
	public void setStar(StellarBody star) {
		this.starId = star.getId();
		this.star = star;
		if(!this.isMoon() && !isStation())
			this.star.addPlanet(this);
	}

	public void setStar(int id) {
		this.starId = id;
		if(DimensionManager.getInstance().getStar(id) != null)
			setStar(DimensionManager.getInstance().getStar(id));
	}

	/**
	 * @return the host star for this planet
	 */
	public StellarBody getStar() {
		if(star == null)
			star = DimensionManager.getInstance().getStar(starId);
		return star;
	}

	public boolean isGasGiant() {
		return isGasGiant;
	}

	public void setGasGiant() {
		this.isGasGiant = true;
	}

	public boolean hasRings() {
		return this.hasRings;
	}

	public void setHasRings(boolean value) {
		this.hasRings = value;
	}

	//Adds a beacon location to the planet's surface
	public void addBeaconLocation(World world, HashedBlockPosition pos) {
		beaconLocations.add(pos);
		DimensionManager.getInstance().knownPlanets.add(getId());

		//LAAZZY
		if(!world.isRemote)
			PacketHandler.sendToAll(new PacketDimInfo(getId(), this));
	}

	public HashSet<HashedBlockPosition> getBeacons() {
		return beaconLocations;
	}

	//Removes a beacon location to the planet's surface
	public void removeBeaconLocation(World world, HashedBlockPosition pos) {
		beaconLocations.remove(pos);

		if(beaconLocations.isEmpty() && !Configuration.initiallyKnownPlanets.contains(getId()))
			DimensionManager.getInstance().knownPlanets.remove(getId());

		//LAAZZY
		if(!world.isRemote)
			PacketHandler.sendToAll(new PacketDimInfo(getId(), this));
	}

	/**
	 * @return the {@link ResourceLocation} representing this planet, generated from the planet's properties
	 */
	public ResourceLocation getPlanetIcon() {


		if(!customIcon.isEmpty())
		{
			try {
				return PlanetIcons.valueOf(customIcon.toUpperCase()).resource;
			} catch(IllegalArgumentException e) {
				return PlanetIcons.UNKNOWN.resource;
			}

		}

		AtmosphereTypes atmType = AtmosphereTypes.getAtmosphereTypeFromValue(atmosphereDensity);
		Temps tempType = Temps.getTempFromValue(averageTemperature);

		if(isGasGiant())
			return PlanetIcons.GASGIANTBLUE.resource;

		if(tempType == Temps.TOOHOT)
			return PlanetIcons.MARSLIKE.resource;
		if(atmType != AtmosphereTypes.NONE && VulpineMath.isBetween(tempType.ordinal(), Temps.COLD.ordinal(), Temps.TOOHOT.ordinal()))
			return PlanetIcons.EARTHLIKE.resource;//TODO: humidity
		else if(tempType.compareTo(Temps.COLD) > 0)
			if(atmType.compareTo(AtmosphereTypes.LOW) > 0)
				return PlanetIcons.MOON.resource;
			else
				return PlanetIcons.ICEWORLD.resource;
		else if(atmType.compareTo(AtmosphereTypes.LOW) > 0) {

			if(tempType.compareTo(Temps.COLD) < 0)
				return PlanetIcons.MARSLIKE.resource;
			else
				return PlanetIcons.MOON.resource;
		}
		else
			return PlanetIcons.LAVA.resource;
	}

	/**
	 * @return the {@link ResourceLocation} representing this planet, generated from the planet's properties
	 */
	public ResourceLocation getPlanetIconLEO() {

		if(!customIcon.isEmpty())
		{
			try {
				return PlanetIcons.valueOf(customIcon.toUpperCase()).resourceLEO;
			} catch(IllegalArgumentException e) {
				return PlanetIcons.UNKNOWN.resource;
			}
		}

		AtmosphereTypes atmType = AtmosphereTypes.getAtmosphereTypeFromValue(atmosphereDensity);
		Temps tempType = Temps.getTempFromValue(averageTemperature);


		if(isGasGiant())
			return PlanetIcons.GASGIANTBLUE.resourceLEO;

		if(tempType == Temps.TOOHOT)
			return PlanetIcons.MARSLIKE.resourceLEO;
		if(atmType != AtmosphereTypes.NONE && VulpineMath.isBetween(tempType.ordinal(), Temps.COLD.ordinal(), Temps.TOOHOT.ordinal()))
			return PlanetIcons.EARTHLIKE.resourceLEO;//TODO: humidity
		else if(tempType.compareTo(Temps.COLD) > 0)
			if(atmType.compareTo(AtmosphereTypes.LOW) > 0)
				return PlanetIcons.MOON.resourceLEO;
			else
				return PlanetIcons.ICEWORLD.resourceLEO;
		else if(atmType.compareTo(AtmosphereTypes.LOW) > 0) {

			if(tempType.compareTo(Temps.COLD) < 0)
				return PlanetIcons.MARSLIKE.resourceLEO;
			else
				return PlanetIcons.MOON.resourceLEO;
		}
		else
			return PlanetIcons.LAVA.resourceLEO;
	}

	/**
	 * @return the name of the planet
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the planet
	 */
	public void setName(String name) {
		this.name = name;
	}

	//Planet hierarchy

	/**
	 * @return the DIMID of the planet
	 */
	public int getId() {
		return planetId;
	}

	/**
	 * @return the DimID of the parent planet
	 */
	public int getParentPlanet() {
		return parentPlanet;
	}

	/**
	 * @return the {@link DimensionProperties} of the parent planet
	 */
	public DimensionProperties getParentProperties() {
		if(parentPlanet != -1)
			return DimensionManager.getInstance().getDimensionProperties(parentPlanet);
		return null;
	}

	/**
	 * Range 0 < value <= 200
	 * @return if the planet is a moon, then the distance from the host planet where the earth's moon is 100, higher is farther, if planet, distance from the star, 100 is earthlike, higher value is father
	 */
	public int getParentOrbitalDistance() {
		return orbitalDist;
	}

	/**
	 * @return if a planet, the same as getParentOrbitalDistance(), if a moon, the moon's distance from the host star
	 */
	public int getSolarOrbitalDistance() {
		if(parentPlanet != -1)
			return getParentProperties().getSolarOrbitalDistance();
		return orbitalDist;
	}

	public double getSolarTheta() {
		if(parentPlanet != -1)
			return getParentProperties().getSolarTheta();
		return orbitTheta;
	}

	/**
	 * Sets this planet as a moon of the supplied planet's id.
	 * @param parentId parent planet's DIMID, or -1 for none
	 */
	public void setParentPlanet(DimensionProperties parent) {
		this.setParentPlanet(parent, true);
	}

	/**
	 * Sets this planet as a moon of the supplied planet's ID
	 * @param parentId DIMID of the parent planet
	 * @param update true to update the parent's planet to the change
	 */
	public void setParentPlanet(DimensionProperties parent, boolean update) {

		if(update) {
			if(parentPlanet != -1)
				getParentProperties().childPlanets.remove(new Integer(getId()));

			if(parent == null) {
				parentPlanet = -1;
			}
			else {
				parentPlanet = parent.getId();
				star = parent.getStar();
				if(parent.getId() != -1)
					parent.childPlanets.add(getId());
			}
		}
		else {
			if(parent == null) {
				parentPlanet = -1;
			}
			else {
				star = parent.getStar();
				parentPlanet = parent.getId();
			}
		}
	}

	/**
	 * @return true if the planet has moons
	 */
	public boolean hasChildren() {
		return !childPlanets.isEmpty();
	}

	/**
	 * @return true if this DIM orbits another
	 */
	public boolean isMoon() {
		return parentPlanet != -1 && parentPlanet != SpaceObjectManager.WARPDIMID;
	}

	/**
	 * @return true if terraformed
	 */
	public boolean isTerraformed() {
		return isTerraformed;
	}

	public int getAtmosphereDensity() {
		return atmosphereDensity;
	}

	public void setAtmosphereDensity(int atmosphereDensity) {

		int prevAtm = this.atmosphereDensity;
		this.atmosphereDensity = atmosphereDensity;

		if (AtmosphereTypes.getAtmosphereTypeFromValue(prevAtm) != AtmosphereTypes.getAtmosphereTypeFromValue(this.atmosphereDensity)) {
			setTerraformedBiomes(getViableBiomes());
			isTerraformed = true;

			((ChunkManagerPlanet)((WorldProviderPlanet)net.minecraftforge.common.DimensionManager.getProvider(getId())).chunkMgrTerraformed).resetCache();

		}

		PacketHandler.sendToAll(new PacketDimInfo(getId(), this));
	}

	public void setAtmosphereDensityDirect(int atmosphereDensity) {
		originalAtmosphereDensity = this.atmosphereDensity = atmosphereDensity;
	}

	/**
	 * 
	 * @return true if the dimension properties refer to that of a space station or orbiting object registered in {@link SpaceObjectManager}
	 */
	public boolean isStation() {
		return isStation;
	}

	//TODO: allow for more exotic atmospheres
	/**
	 * @return the default atmosphere of this dimension
	 */
	public IAtmosphere getAtmosphere() {
		if(hasAtmosphere()) {
			if(AtmosphereTypes.getAtmosphereTypeFromValue(getAtmosphereDensity()) == AtmosphereTypes.HIGHPRESSURE)
				return AtmosphereType.HIGHPRESSURE;
			return AtmosphereType.AIR;
		}
		return AtmosphereType.VACUUM;
	}

	/**
	 * @return {@link ResourceLocation} refering to the image to render as atmospheric haze as seen from orbit
	 */
	public static ResourceLocation getAtmosphereResource() {
		return atmosphere;
	}

	public static ResourceLocation getShadowResource() {
		return shadow;
	}


	public static ResourceLocation getAtmosphereLEOResource() {
		return atmosphereLEO;
	}

	/**
	 * @return true if the planet has an atmosphere
	 */
	public boolean hasAtmosphere() {
		return AtmosphereTypes.getAtmosphereTypeFromValue(atmosphereDensity).compareTo(AtmosphereTypes.NONE) < 0;
	}

	/**
	 * @return set of all moons orbiting this planet
	 */
	public Set<Integer> getChildPlanets() {
		return childPlanets;
	}

	/**
	 * @return how many moons deep this planet is, IE: if the moon of a moon of a planet then three is returned
	 */
	public int getPathLengthToStar() {
		if(isMoon())
			return 1 + getParentProperties().getPathLengthToStar();
		return 1;
	}

	/**
	 * Does not check for hierarchy loops!
	 * @param id DIMID of the new child
	 * @return true if successfully added as a child planet
	 */
	public boolean addChildPlanet(DimensionProperties child) {
		//TODO: check for hierarchy loops!
		if(child == this)
			return false;

		childPlanets.add(child.getId());
		child.setParentPlanet(this);
		return true;
	}

	/**
	 * Removes the passed DIMID from the list of moons
	 * @param id
	 */
	public void removeChild(int id) {
		childPlanets.remove(id);
	}

	//Satallites --------------------------------------------------------
	/**
	 * Adds a satellite to this DIM
	 * @param satellite satellite to add
	 * @param world world to add the satellite to
	 */
	public void addSatallite(SatelliteBase satellite, World world) {
		//Prevent dupes
		if(satallites.containsKey(satellite.getId())) {
			satallites.remove(satellite.getId());
			tickingSatallites.remove(satellite.getId());
		}

		satallites.put(satellite.getId(), satellite);
		satellite.setDimensionId(world);


		if(satellite.canTick())
			tickingSatallites.put(satellite.getId(),satellite);

		if(!world.isRemote)
			PacketHandler.sendToAll(new PacketSatellite(satellite));
	}

	/**
	 * Really only meant to be used on the client when recieving a packet
	 * @param satallite
	 */
	public void addSatallite(SatelliteBase satallite) {
		if(satallites.containsKey(satallite.getId())) {
			satallites.remove(satallite.getId());
			tickingSatallites.remove(satallite.getId());
		}
		satallites.put(satallite.getId(), satallite);

		if(satallite.canTick()) //TODO: check for dupes
			tickingSatallites.put(satallite.getId(), satallite);
	}

	/**
	 * Removes the satellite from orbit around this world
	 * @param satalliteId ID # for this satellite
	 * @return reference to the satellite object
	 */
	public SatelliteBase removeSatellite(long satalliteId) {
		SatelliteBase satallite = satallites.remove(satalliteId);

		if(satallite != null && satallite.canTick() && tickingSatallites.containsKey(satalliteId))
			tickingSatallites.get(satalliteId).setDead();

		return satallite;
	}

	/**
	 * @param id ID # for this satellite
	 * @return a reference to the satelliteBase object given this ID
	 */
	public SatelliteBase getSatellite(long id) {
		return satallites.get(id);
	}

	//TODO: multithreading
	/**
	 * Tick satellites as needed
	 */
	public void tick() {

		Iterator<SatelliteBase> iterator = tickingSatallites.values().iterator();

		while(iterator.hasNext()) {
			SatelliteBase satallite = iterator.next();
			satallite.tickEntity();

			if(satallite.isDead()) {
				iterator.remove();
				satallites.remove(satallite.getId());
			}
		}
		updateOrbit();
	}

	public void updateOrbit() {
		this.prevOrbitalTheta = this.orbitTheta;
		this.orbitTheta = (AdvancedRocketry.proxy.getWorldTimeUniversal(0)*(201-orbitalDist)*0.000002d) % (2*Math.PI);
	}

	/**
	 * @return true if this dimension is allowed to have rivers
	 */
	public boolean hasRivers() {
		return AtmosphereTypes.getAtmosphereTypeFromValue(originalAtmosphereDensity).compareTo(AtmosphereTypes.LOW) <= 0 && Temps.getTempFromValue(averageTemperature).isInRange(Temps.COLD, Temps.HOT);
	}


	/**
	 * Each Planet is assigned a list of biomes that are allowed to spawn there
	 * @return List of biomes allowed to spawn on this planet
	 */
	public List<BiomeEntry> getBiomes() {
		return (List<BiomeEntry>)allowedBiomes;
	}

	public List<BiomeEntry> getTerraformedBiomes() {
		return (List<BiomeEntry>)terraformedBiomes;
	}

	/**
	 * Used to determine if a biome is allowed to spawn on ANY planet
	 * @param biome biome to check
	 * @return true if the biome is not allowed to spawn on any Dimension
	 */
	public boolean isBiomeblackListed(Biome biome) {
		return AdvancedRocketryBiomes.instance.getBlackListedBiomes().contains(Biome.getIdForBiome(biome));
	}

	/**
	 * @return a list of biomes allowed to spawn in this dimension
	 */
	public List<Biome> getViableBiomes() {
		Random random = new Random(System.nanoTime());
		List<Biome> viableBiomes = new ArrayList<Biome>();

		if(atmosphereDensity > AtmosphereTypes.LOW.value && random.nextInt(3) == 0) {
			List<Biome> list = new LinkedList<Biome>(AdvancedRocketryBiomes.instance.getSingleBiome());

			while(list.size() > 1) {
				Biome biome = list.get(random.nextInt(list.size()));
				Temps temp = Temps.getTempFromValue(averageTemperature);
				if((biome.getTempCategory() == TempCategory.COLD && temp.isInRange(Temps.FRIGID, Temps.NORMAL)) ||
						((biome.getTempCategory() == TempCategory.MEDIUM || biome.getTempCategory() == TempCategory.OCEAN) &&
								temp.isInRange(Temps.COLD, Temps.HOT)) ||
								(biome.getTempCategory() == TempCategory.WARM && temp.isInRange(Temps.NORMAL, Temps.HOT))) {
					viableBiomes.add(biome);
					return viableBiomes;
				}
				list.remove(biome);
			}
		}


		if(atmosphereDensity <= AtmosphereTypes.LOW.value)
			viableBiomes.add(AdvancedRocketryBiomes.moonBiome);

		else if(averageTemperature > Temps.TOOHOT.getTemp()) {
			viableBiomes.add(AdvancedRocketryBiomes.hotDryBiome);
		}
		else if(averageTemperature > Temps.HOT.getTemp()) {

			Iterator<Biome> itr = Biome.REGISTRY.iterator();
			while( itr.hasNext()) {
				Biome biome = itr.next();
				if(biome != null && (BiomeDictionary.getTypes(biome).contains(BiomeDictionary.Type.HOT) || BiomeDictionary.getTypes(biome).contains(BiomeDictionary.Type.OCEAN))  && !isBiomeblackListed(biome)) {
					viableBiomes.add(biome);
				}
			}
		}
		else if(averageTemperature > Temps.NORMAL.getTemp()) {
			Iterator<Biome> itr = Biome.REGISTRY.iterator();
			while( itr.hasNext()) {
				Biome biome = itr.next();
				if(biome != null && !BiomeDictionary.getTypes(biome).contains(BiomeDictionary.Type.COLD) && !isBiomeblackListed(biome)) {
					viableBiomes.add(biome);
				}
			}
			viableBiomes.addAll(BiomeDictionary.getBiomes(BiomeDictionary.Type.OCEAN));
		}
		else if(averageTemperature > Temps.COLD.getTemp()) {
			Iterator<Biome> itr = Biome.REGISTRY.iterator();
			while( itr.hasNext()) {
				Biome biome = itr.next();
				if(biome != null && !BiomeDictionary.getTypes(biome).contains(BiomeDictionary.Type.HOT) && !isBiomeblackListed(biome)) {
					viableBiomes.add(biome);
				}
			}
			viableBiomes.addAll(BiomeDictionary.getBiomes(BiomeDictionary.Type.OCEAN));
		}
		else if(averageTemperature > Temps.FRIGID.getTemp()) {

			Iterator<Biome> itr = Biome.REGISTRY.iterator();
			while( itr.hasNext()) {
				Biome biome = itr.next();
				if(biome != null && !BiomeDictionary.getTypes(biome).contains(BiomeDictionary.Type.COLD) && !isBiomeblackListed(biome)) {
					viableBiomes.add(biome);
				}
			}
		}
		else {//(averageTemperature >= Temps.SNOWBALL.getTemp())
			Iterator<Biome> itr = Biome.REGISTRY.iterator();
			while( itr.hasNext()) {
				Biome biome = itr.next();
				if(biome != null && !BiomeDictionary.getTypes(biome).contains(BiomeDictionary.Type.COLD) && !isBiomeblackListed(biome)) {
					viableBiomes.add(biome);
				}
			}
			//TODO:
		}

		if(viableBiomes.size() > 5) {
			viableBiomes  = ZUtils.copyRandomElements(viableBiomes, 5);
		}

		if(atmosphereDensity > AtmosphereTypes.HIGHPRESSURE.value && Temps.getTempFromValue(averageTemperature).isInRange(Temps.NORMAL, Temps.HOT))
			viableBiomes.addAll(AdvancedRocketryBiomes.instance.getHighPressureBiomes());

		return viableBiomes;
	}

	/**
	 * Adds a biome to the list of biomes allowed to spawn on this planet
	 * @param biome biome to be added as viable
	 */
	public void addBiome(Biome biome) {
		ArrayList<Biome> biomes = new ArrayList<Biome>();
		biomes.add(biome);
		allowedBiomes.addAll(getBiomesEntries(biomes));
	}

	/**
	 * Adds a biome to the list of biomes allowed to spawn on this planet
	 * @param biome biome to be added as viable
	 * @return true if the biome was added sucessfully, false otherwise
	 */
	public boolean addBiome(int biomeId) {

		Biome biome =  Biome.getBiome(biomeId);
		if(biomeId == 0 || Biome.getIdForBiome(biome) != 0) {
			List<Biome> biomes = new ArrayList<Biome>();
			biomes.add(biome);
			allowedBiomes.addAll(getBiomesEntries(biomes));
			return true;
		}
		return false;
	}

	/**
	 * Adds a list of biomes to the allowed list of biomes for this planet
	 * @param biomes 
	 */
	public void addBiomes(List<Biome> biomes) {
		//TODO check for duplicates
		allowedBiomes.addAll(getBiomesEntries(biomes));
	}

	/**
	 * Clears the list of allowed biomes and replaces it with the provided list
	 * @param biomes
	 */
	public void setBiomes(List<Biome> biomes) {
		allowedBiomes.clear();
		addBiomes(biomes);
	}

	public void setBiomeEntries(List<BiomeEntry> biomes) {
		//If list is itself DO NOT CLEAR IT
		if(biomes != allowedBiomes) {
			allowedBiomes.clear();
			allowedBiomes.addAll(biomes);
		}
	}

	public void setTerraformedBiomes(List<Biome> biomes) {
		terraformedBiomes.clear();
		terraformedBiomes.addAll(getBiomesEntries(biomes));
	}

	/**
	 * Adds all biomes of this type to the list of biomes allowed to generate
	 * @param type
	 */
	public void addBiomeType(BiomeDictionary.Type type) {

		ArrayList<Biome> entryList = new ArrayList<Biome>();

		entryList.addAll(BiomeDictionary.getBiomes(type));

		//Neither are acceptable on planets
		entryList.remove(Biome.getBiome(8));
		entryList.remove(Biome.getBiome(9));

		//Make sure we dont add double entries
		Iterator<Biome> iter = entryList.iterator();
		while(iter.hasNext()) {
			Biome nextbiome = iter.next();
			for(BiomeEntry entry : allowedBiomes) {
				if(BiomeDictionary.areSimilar(entry.biome, nextbiome))
					iter.remove();
			}

		}
		allowedBiomes.addAll(getBiomesEntries(entryList));

	}

	/**
	 * Removes all biomes of this type from the list of biomes allowed to generate
	 * @param type
	 */
	public void removeBiomeType(BiomeDictionary.Type type) {

		ArrayList<Biome> entryList = new ArrayList<Biome>();

		entryList.addAll(BiomeDictionary.getBiomes(type));

		Iterator<Biome> itr = Biome.REGISTRY.iterator();
		while(itr.hasNext()) {
			Biome biome = itr.next();
			Iterator<BiomeEntry> iterator = allowedBiomes.iterator();
			while(iterator.hasNext()) {
				if(BiomeDictionary.areSimilar(iterator.next().biome, biome))
					iterator.remove();
			}
		}

	}

	/**
	 * Gets a list of BiomeEntries allowed to spawn in this dimension
	 * @param biomeIds
	 * @return
	 */
	private ArrayList<BiomeEntry> getBiomesEntries(List<Biome> biomeIds) {

		ArrayList<BiomeEntry> biomeEntries = new ArrayList<BiomeManager.BiomeEntry>();

		Iterator<Biome> itr = biomeIds.iterator();
		while( itr.hasNext()) {
			Biome biomes = itr.next();

			/*if(biomes == Biome.desert) {
				biomeEntries.add(new BiomeEntry(BiomeGenBase.desert, 30));
				continue;
			}
			else if(biomes == BiomeGenBase.savanna) {
				biomeEntries.add(new BiomeEntry(BiomeGenBase.savanna, 20));
				continue;
			}
			else if(biomes == BiomeGenBase.plains) {
				biomeEntries.add(new BiomeEntry(BiomeGenBase.plains, 10));
				continue;
			}*/

			boolean notFound = true;

			label:

				for(BiomeManager.BiomeType types : BiomeManager.BiomeType.values()) {
					for(BiomeEntry entry : BiomeManager.getBiomes(types)) {
						if(biomes == null)
							AdvancedRocketry.logger.warn("Null biomes loaded for DIMID: " + this.getId());
						else if(entry.biome.equals(biomes)) {
							biomeEntries.add(entry);
							notFound = false;

							break label;
						}
					}
				}

			if(notFound && biomes != null) {
				biomeEntries.add(new BiomeEntry(biomes, 30));
			}
		}

		return biomeEntries;
	}

	public void readFromNBT(NBTTagCompound nbt) {
		NBTTagList list;

		if(nbt.hasKey("skyColor")) {
			list = nbt.getTagList("skyColor", NBT.TAG_FLOAT);
			skyColor = new float[list.tagCount()];
			for(int f = 0 ; f < list.tagCount(); f++) {
				skyColor[f] = list.getFloatAt(f);
			}
		}

		if(nbt.hasKey("ringColor")) {
			list = nbt.getTagList("ringColor", NBT.TAG_FLOAT);
			ringColor = new float[list.tagCount()];
			for(int f = 0 ; f < list.tagCount(); f++) {
				ringColor[f] = list.getFloatAt(f);
			}
		}

		if(nbt.hasKey("sunriseSunsetColors")) {
			list = nbt.getTagList("sunriseSunsetColors", NBT.TAG_FLOAT);
			sunriseSunsetColors = new float[list.tagCount()];
			for(int f = 0 ; f < list.tagCount(); f++) {
				sunriseSunsetColors[f] = list.getFloatAt(f);
			}
		}

		if(nbt.hasKey("fogColor")) {
			list = nbt.getTagList("fogColor", NBT.TAG_FLOAT);
			fogColor = new float[list.tagCount()];
			for(int f = 0 ; f < list.tagCount(); f++) {
				fogColor[f] = list.getFloatAt(f);
			}
		}

		//Load biomes
		if(nbt.hasKey("biomes")) {

			allowedBiomes.clear();
			int biomeIds[] = nbt.getIntArray("biomes");
			List<Biome> biomesList = new ArrayList<Biome>();


			for(int i = 0; i < biomeIds.length; i++) {
				biomesList.add(AdvancedRocketryBiomes.instance.getBiomeById(biomeIds[i]));
			}

			allowedBiomes.addAll(getBiomesEntries(biomesList));
		}

		if(nbt.hasKey("beaconLocations")) {
			list = nbt.getTagList("beaconLocations", NBT.TAG_INT_ARRAY);

			for(int i = 0 ; i < list.tagCount(); i++) {
				int[] location = list.getIntArrayAt(i);
				beaconLocations.add(new HashedBlockPosition(location[0], location[1], location[2]));
			}
			DimensionManager.getInstance().knownPlanets.add(getId());
		}
		else
			beaconLocations.clear();

		//Load biomes
		if(nbt.hasKey("biomesTerra")) {

			terraformedBiomes.clear();
			int biomeIds[] = nbt.getIntArray("biomesTerra");
			List<Biome> biomesList = new ArrayList<Biome>();


			for(int i = 0; i < biomeIds.length; i++) {
				biomesList.add(AdvancedRocketryBiomes.instance.getBiomeById(biomeIds[i]));
			}

			terraformedBiomes.addAll(getBiomesEntries(biomesList));
		}


		gravitationalMultiplier = nbt.getFloat("gravitationalMultiplier");
		orbitalDist = nbt.getInteger("orbitalDist");
		orbitTheta = nbt.getDouble("orbitTheta");
		orbitalPhi = nbt.getDouble("orbitPhi");
		rotationalPhi = nbt.getDouble("rotationalPhi");
		atmosphereDensity = nbt.getInteger("atmosphereDensity");

		if(nbt.hasKey("originalAtmosphereDensity"))
			originalAtmosphereDensity = nbt.getInteger("originalAtmosphereDensity");
		else 
			originalAtmosphereDensity = atmosphereDensity;

		averageTemperature = nbt.getInteger("avgTemperature");
		rotationalPeriod = nbt.getInteger("rotationalPeriod");
		name = nbt.getString("name");
		isNativeDimension = nbt.hasKey("isNative") ? nbt.getBoolean("isNative") : true; //Prevent world breakages when loading from old version
		isGasGiant = nbt.getBoolean("isGasGiant");
		isTerraformed = nbt.getBoolean("terraformed");
		hasRings = nbt.getBoolean("hasRings");
		sealevel = nbt.getInteger("sealevel");

		//Hierarchy
		if(nbt.hasKey("childrenPlanets")) {
			for(int i : nbt.getIntArray("childrenPlanets"))
				childPlanets.add(i);
		}

		//Note: parent planet must be set before setting the star otherwise it would cause duplicate planets in the StellarBody's array
		parentPlanet = nbt.getInteger("parentPlanet");
		this.setStar(DimensionManager.getInstance().getStar(nbt.getInteger("starId")));

		//Satallites

		if(nbt.hasKey("satallites")) {
			NBTTagCompound allSatalliteNbt = nbt.getCompoundTag("satallites");

			for(Object keyObject : allSatalliteNbt.getKeySet()) {
				String key = (String)keyObject;
				Long longKey = Long.parseLong(key);

				NBTTagCompound satalliteNbt = allSatalliteNbt.getCompoundTag(key);

				if(satallites.containsKey(longKey)){
					satallites.get(longKey).readFromNBT(satalliteNbt);
				} 
				else {
					//Check for NBT errors
					try {
						SatelliteBase satallite = SatelliteRegistry.createFromNBT(satalliteNbt);

						satallites.put(longKey, satallite);

						if(satallite.canTick()) {
							tickingSatallites.put(satallite.getId(), satallite);
						}

					} catch (NullPointerException e) {
						AdvancedRocketry.logger.warn("Satellite with bad NBT detected, Removing");
					}
				}
			}
		}

		if(isGasGiant) {
			NBTTagList fluidList = nbt.getTagList("fluids", NBT.TAG_STRING);
			getHarvestableGasses().clear();

			for(int i = 0; i < fluidList.tagCount(); i++) {
				Fluid f = FluidRegistry.getFluid(fluidList.getStringTagAt(i));
				if(f != null)
					getHarvestableGasses().add(f);
			}

			//Do not allow empty atmospheres, at least not yet
			if(getHarvestableGasses().isEmpty())
				getHarvestableGasses().addAll(AtmosphereRegister.getInstance().getHarvestableGasses());
		}

		if(nbt.hasKey("oceanBlock")) {
			Block block = Block.REGISTRY.getObject(new ResourceLocation(nbt.getString("oceanBlock")));
			if(block == Blocks.AIR) {
				oceanBlock = null;
			}
			else {
				int meta = nbt.getInteger("oceanBlockMeta");
				oceanBlock = block.getStateFromMeta(meta);
			}
		}
		else
			oceanBlock = null;
	}

	public void writeToNBT(NBTTagCompound nbt) {
		NBTTagList list;

		if(skyColor != null) {
			list = new NBTTagList();
			for(float f : skyColor) {
				list.appendTag(new NBTTagFloat(f));
			}
			nbt.setTag("skyColor", list);
		}

		if(sunriseSunsetColors != null) {
			list = new NBTTagList();
			for(float f : sunriseSunsetColors) {
				list.appendTag(new NBTTagFloat(f));
			}
			nbt.setTag("sunriseSunsetColors", list);
		}

		list = new NBTTagList();
		for(float f : fogColor) {
			list.appendTag(new NBTTagFloat(f));
		}
		nbt.setTag("fogColor", list);

		if(hasRings) {
			list = new NBTTagList();
			for(float f : ringColor) {
				list.appendTag(new NBTTagFloat(f));
			}
			nbt.setTag("ringColor", list);
		}

		if(!beaconLocations.isEmpty()) {
			list = new NBTTagList();

			for(HashedBlockPosition pos : beaconLocations) {
				list.appendTag(new NBTTagIntArray(new int[] {pos.x, pos.y, pos.z}));
			}
			nbt.setTag("beaconLocations", list);
		}


		if(!allowedBiomes.isEmpty()) {
			int biomeId[] = new int[allowedBiomes.size()];
			for(int i = 0; i < allowedBiomes.size(); i++) {
				biomeId[i] = Biome.getIdForBiome(allowedBiomes.get(i).biome);
			}
			nbt.setIntArray("biomes", biomeId);
		}

		if(!terraformedBiomes.isEmpty()) {
			int biomeId[] = new int[terraformedBiomes.size()];
			for(int i = 0; i < terraformedBiomes.size(); i++) {

				biomeId[i] = Biome.getIdForBiome(terraformedBiomes.get(i).biome);
			}
			nbt.setIntArray("biomesTerra", biomeId);
		}



		nbt.setInteger("starId", starId);
		nbt.setFloat("gravitationalMultiplier", gravitationalMultiplier);
		nbt.setInteger("orbitalDist", orbitalDist);
		nbt.setDouble("orbitTheta", orbitTheta);
		nbt.setDouble("orbitPhi", orbitalPhi);
		nbt.setDouble("rotationalPhi", rotationalPhi);
		nbt.setInteger("atmosphereDensity", atmosphereDensity);
		nbt.setInteger("originalAtmosphereDensity", originalAtmosphereDensity);
		nbt.setInteger("avgTemperature", averageTemperature);
		nbt.setInteger("rotationalPeriod", rotationalPeriod);
		nbt.setString("name", name);
		nbt.setBoolean("isNative", isNativeDimension);
		nbt.setBoolean("terraformed", isTerraformed);
		nbt.setBoolean("isGasGiant", isGasGiant);
		nbt.setBoolean("hasRings", hasRings);
		nbt.setInteger("sealevel", sealevel);

		//Hierarchy
		if(!childPlanets.isEmpty()) {
			Integer intList[] = new Integer[childPlanets.size()];

			NBTTagIntArray childArray = new NBTTagIntArray(ArrayUtils.toPrimitive(childPlanets.toArray(intList)));
			nbt.setTag("childrenPlanets", childArray);
		}

		nbt.setInteger("parentPlanet", parentPlanet);

		//Satallites

		if(!satallites.isEmpty()) {
			NBTTagCompound allSatalliteNbt = new NBTTagCompound();
			for(Entry<Long, SatelliteBase> entry : satallites.entrySet()) {
				NBTTagCompound satalliteNbt = new NBTTagCompound();

				entry.getValue().writeToNBT(satalliteNbt);
				allSatalliteNbt.setTag(entry.getKey().toString(), satalliteNbt);
			}
			nbt.setTag("satallites", allSatalliteNbt);
		}

		if(isGasGiant) {
			NBTTagList fluidList = new NBTTagList();

			for(Fluid f : getHarvestableGasses()) {
				fluidList.appendTag(new NBTTagString(f.getName()));
			}

			nbt.setTag("fluids", fluidList);
		}

		if(oceanBlock != null) {
			nbt.setString("oceanBlock", Block.REGISTRY.getNameForObject(oceanBlock.getBlock()).toString());
			nbt.setInteger("oceanBlockMeta", oceanBlock.getBlock().getMetaFromState(oceanBlock));
		}

	}

	public IBlockState getOceanBlock() {
		return oceanBlock;
	}

	public void setOceanBlock(IBlockState block) {
		oceanBlock = block;
	}


	public static DimensionProperties createFromNBT(int id, NBTTagCompound nbt) {
		DimensionProperties properties = new DimensionProperties(id);
		properties.readFromNBT(nbt);
		properties.planetId = id;

		return properties;
	}

	/**
	 * Function for calculating atmosphere thinning with respect to hieght, normallized
	 * @param y
	 * @return the density of the atmosphere at the given height
	 */
	public float getAtmosphereDensityAtHeight(double y) {
		return atmosphereDensity*MathHelper.clamp((float) ( 1 + (256 - y)/200f), 0f,1f)/100f;
	}

	/**
	 * Gets the fog color at a given altitude, used to assist the illusion of thinning atmosphere
	 * @param y
	 * @param fogColor current fog color at this location
	 * @return
	 */
	public float[] getFogColorAtHeight(double y, Vec3d fogColor) {
		float atmDensity = getAtmosphereDensityAtHeight(y);
		return new float[] { (float) (atmDensity * fogColor.x), (float) (atmDensity * fogColor.y), (float) (atmDensity * fogColor.z) };
	}

	/**
	 * Sets the planet's id
	 * @param id
	 */
	public void setId(int id) {
		this.planetId = id;
	}

	@Override
	public void setParentOrbitalDistance(int distance) {
		this.orbitalDist = distance;

	}

	public int getStarId() {
		return starId;
	}

	@Override
	public String toString() {
		return String.format("Dimension ID: %d.  Dimension Name: %s.  Parent Star %d ", getId(), getName(), getStarId());
	}

	@Override
	public double getOrbitTheta() {
		return orbitTheta;
	}

	@Override
	public int getOrbitalDist() {
		return orbitalDist;
	}
	
	public int getSeaLevel() {
		return sealevel;
	}
	
	public void setSeaLevel(int sealevel) {
		this.sealevel = MathHelper.clamp(sealevel, 0, 255);
	}
}