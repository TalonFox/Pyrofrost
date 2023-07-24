package sh.talonfox.pyrofrost.temperature;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameRules;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.PalettedContainer;
import sh.talonfox.pyrofrost.Pyrofrost;
import sh.talonfox.pyrofrost.network.UpdateTemperature;

import java.util.HashMap;
import java.util.Map;

public class Temperature {
    private int wetness;
    private float thirst = 10F;
    private float coreTemp = 1.634457832F;
    private float skinTemp = 1.634457832F;
    private TemperatureDirection skinTempDir = TemperatureDirection.NONE;
    private ServerPlayerEntity serverPlayer;
    private boolean isServerSide;
    private double envRadiation;
    private int ticks = 0;
    private double wbgt;
    private static HashMap<TagKey<Biome>, Float> temperature = new HashMap<>();
    private static HashMap<TagKey<Biome>, Float> humidity = new HashMap<>();
    private static HashMap<TagKey<Biome>, Float> dayNightOffset = new HashMap<>();

    public static final float LOW = 1.554216868F;
    public static final float LOW_WARNING3 = 1.572048193F;
    public static final float LOW_WARNING2 = 1.589879518F;
    public static final float LOW_WARNING1 = 1.612168675F;
    public static final float NORMAL = 1.634457832F;
    public static final float HIGH_WARNING1 = 1.700210844F;
    public static final float HIGH_WARNING2 = 1.765963856F;
    public static final float HIGH_WARNING3 = 1.7826807235F;
    public static final float HIGH = 1.799397591F;

    public enum TemperatureDirection {

        WARMING(0.025F),
        WARMING_NORMALLY(0.00625F),
        WARMING_RAPIDLY(0.2F),
        NONE(0.0F),
        COOLING(0.0125F),
        COOLING_NORMALLY(0.00625F),
        COOLING_RAPIDLY(0.2F);

        public final float coreRate;

        TemperatureDirection(float coreRate) {
            this.coreRate = coreRate;
        }

    }

    public static TemperatureDirection getCoreTemperatureDirection(float lastSkinTemperature, float coreTemperature, float skinTemperature) {
        TemperatureDirection direction = TemperatureDirection.NONE;

        if (lastSkinTemperature > skinTemperature) {
            direction = TemperatureDirection.COOLING_NORMALLY;

            if (coreTemperature > NORMAL) {
                if (skinTemperature < coreTemperature) {
                    direction = TemperatureDirection.COOLING_RAPIDLY;
                } else {
                    direction = TemperatureDirection.COOLING;
                }
            }
        }
        else if (lastSkinTemperature < skinTemperature) {
            direction = TemperatureDirection.WARMING_NORMALLY;

            if (coreTemperature < NORMAL) {
                if (skinTemperature > coreTemperature) {
                    direction = TemperatureDirection.WARMING_RAPIDLY;
                } else {
                    direction = TemperatureDirection.WARMING;
                }
            }
        }

        return direction;
    }

    public static TemperatureDirection getSkinTemperatureDirection(float localTemperature, float lastSkinTemperature) {
        TemperatureDirection direction = TemperatureDirection.NONE;

        if (lastSkinTemperature > NORMAL) {
            if (localTemperature > 1.220F) {
                direction = TemperatureDirection.WARMING_NORMALLY;

                if (localTemperature > 1.888F) {
                    direction = TemperatureDirection.WARMING;
                }
            }
            else if (localTemperature < 1.888F){
                direction = TemperatureDirection.COOLING;

                if (localTemperature < 0.997F) {
                    direction = TemperatureDirection.COOLING_RAPIDLY;
                }
            }
        }
        else if (lastSkinTemperature < NORMAL) {
            if (localTemperature > 0.997F) {
                direction = TemperatureDirection.WARMING_NORMALLY;

                if (localTemperature > 2.557F) {
                    direction = TemperatureDirection.WARMING_RAPIDLY;
                }
                else if (localTemperature > 1.220F) {
                    direction = TemperatureDirection.WARMING;
                }
            }
            else {
                direction = TemperatureDirection.COOLING_NORMALLY;
            }
        }
        else {
            if (localTemperature > 1.220F) {
                direction = TemperatureDirection.WARMING_NORMALLY;
            }
            else if (localTemperature < 0.997F) {
                direction = TemperatureDirection.COOLING_NORMALLY;
            }
        }

        return direction;
    }


    public static void initialize() {
        temperature.put(BiomeTags.IS_BADLANDS,1.309F);
        humidity.put(BiomeTags.IS_BADLANDS,20.0F);
        dayNightOffset.put(BiomeTags.IS_BADLANDS,15F);
        temperature.put(BiomeTags.IS_BEACH,0.663F);
        humidity.put(BiomeTags.IS_BEACH,70.0F);
        dayNightOffset.put(BiomeTags.IS_BEACH,10F);
        temperature.put(BiomeTags.IS_FOREST,0.663F);
        humidity.put(BiomeTags.IS_FOREST,50.0F);
        dayNightOffset.put(BiomeTags.IS_FOREST,12F);
        temperature.put(BiomeTags.IS_END,0.551F);
        humidity.put(BiomeTags.IS_END,40.0F);
        dayNightOffset.put(BiomeTags.IS_END,0F);
        temperature.put(BiomeTags.IS_HILL,0.618F);
        humidity.put(BiomeTags.IS_HILL,50.0F);
        dayNightOffset.put(BiomeTags.IS_HILL,10F);
        temperature.put(BiomeTags.IS_DEEP_OCEAN,0.596F);
        humidity.put(BiomeTags.IS_DEEP_OCEAN,70.0F);
        dayNightOffset.put(BiomeTags.IS_DEEP_OCEAN,5F);
        temperature.put(BiomeTags.IS_OCEAN,0.640F);
        humidity.put(BiomeTags.IS_OCEAN,70.0F);
        dayNightOffset.put(BiomeTags.IS_OCEAN,10F);
        temperature.put(BiomeTags.IS_MOUNTAIN,0.618F);
        humidity.put(BiomeTags.IS_MOUNTAIN,50.0F);
        dayNightOffset.put(BiomeTags.IS_MOUNTAIN,10F);
        temperature.put(BiomeTags.IS_JUNGLE,0.997F);
        humidity.put(BiomeTags.IS_JUNGLE,90.0F);
        dayNightOffset.put(BiomeTags.IS_JUNGLE,15F);
        temperature.put(BiomeTags.IS_NETHER,1.666F);
        humidity.put(BiomeTags.IS_NETHER,20.0F);
        dayNightOffset.put(BiomeTags.IS_NETHER,0F);
        temperature.put(BiomeTags.IS_RIVER,0.551F);
        humidity.put(BiomeTags.IS_RIVER,70.0F);
        dayNightOffset.put(BiomeTags.IS_RIVER,10F);
        temperature.put(BiomeTags.IS_SAVANNA,1.108F);
        humidity.put(BiomeTags.IS_SAVANNA,30.0F);
        dayNightOffset.put(BiomeTags.IS_SAVANNA,15F);
        temperature.put(BiomeTags.IS_TAIGA,0.507F);
        humidity.put(BiomeTags.IS_TAIGA,50.0F);
        dayNightOffset.put(BiomeTags.IS_TAIGA,10F);
        temperature.put(BiomeTags.IGLOO_HAS_STRUCTURE,0.507F);
        humidity.put(BiomeTags.IGLOO_HAS_STRUCTURE,20.0F);
        dayNightOffset.put(BiomeTags.IGLOO_HAS_STRUCTURE,5F);
        temperature.put(BiomeTags.VILLAGE_PLAINS_HAS_STRUCTURE,0.774F);
        humidity.put(BiomeTags.VILLAGE_PLAINS_HAS_STRUCTURE,60.0F);
        dayNightOffset.put(BiomeTags.VILLAGE_PLAINS_HAS_STRUCTURE,15F);
        temperature.put(BiomeTags.RUINED_PORTAL_SWAMP_HAS_STRUCTURE,0.685F);
        humidity.put(BiomeTags.RUINED_PORTAL_SWAMP_HAS_STRUCTURE,90.0F);
        dayNightOffset.put(BiomeTags.RUINED_PORTAL_SWAMP_HAS_STRUCTURE,10F);
        temperature.put(BiomeTags.DESERT_PYRAMID_HAS_STRUCTURE,1.354F);
        humidity.put(BiomeTags.DESERT_PYRAMID_HAS_STRUCTURE,20.0F);
        dayNightOffset.put(BiomeTags.DESERT_PYRAMID_HAS_STRUCTURE,20F);
    }

    public Temperature(ServerPlayerEntity player, boolean shouldUpdate) {
        isServerSide = shouldUpdate;
        serverPlayer = player;
    }

    public void tick() {
        if(ticks % 2 == 0) {
            if (this.coreTemp < LOW) {
                serverPlayer.setFrozenTicks(serverPlayer.getFrozenTicks() + 5);
            }
        }
        ticks += 1;
        if(ticks % 16 == 0 || ticks % 60 == 0) {
            this.wbgt = getWBGT();
            boolean sweat = false;
            this.skinTempDir = getSkinTemperatureDirection((float)this.wbgt, this.skinTemp);
            float tempChange = getAirTemperatureSkinChange(this.serverPlayer, this.wbgt < 0.997F ? 4.3F*3F : 0F);
            if (tempChange > 0.0F) {
                switch (skinTempDir) {
                    case COOLING -> {
                        tempChange = Math.max(-(tempChange) * 70.0F, -(0.022289157F * 3.0F));
                    }
                    case COOLING_RAPIDLY -> {
                        tempChange = Math.max(-(tempChange) * 100.0F, -(0.022289157F * 4.0F));
                    }
                    case COOLING_NORMALLY -> {
                        tempChange = -(tempChange);
                        float exhaustion = Math.abs(Math.min(tempChange * 200.0F, 0.2F));
                        serverPlayer.getHungerManager().addExhaustion(exhaustion);
                        sweat = true;
                    }
                    case WARMING -> {
                        if (this.thirst <= 0) {
                            tempChange = Math.min(tempChange * 70.0F, 0.022289157F * 3.0F);
                        } else {
                            this.thirst -= Math.min(tempChange * 150.0F, 0.3F);
                            if (this.thirst < 0) {
                                this.thirst = 0;
                            }
                            sweat = true;
                        }
                    }
                    case WARMING_RAPIDLY -> tempChange = Math.min(tempChange * 100.0F, 0.022289157F * 4.0F);
                    case WARMING_NORMALLY -> {
                        if(this.thirst > 0) {
                            this.thirst -= Math.min(tempChange * 100.0F, 0.1F);
                            if (this.thirst < 0) {
                                this.thirst = 0;
                            }
                            sweat = true;
                        }
                    }
                }
            }
            if (tempChange == 0.0F) {
                if (this.skinTemp < NORMAL) {
                    tempChange = (NORMAL - this.skinTemp) / 20.0F;
                }
                else if (this.skinTemp > NORMAL) {
                    tempChange = -((this.skinTemp - NORMAL) / 40.0F);
                }
            }
            var oldSkinTemp = this.skinTemp;
            this.skinTemp += tempChange;
            final TemperatureDirection coreTempDir = getCoreTemperatureDirection(oldSkinTemp, this.coreTemp, this.skinTemp);
            float diff = Math.abs(this.skinTemp - this.coreTemp);
            float change;
            if (coreTempDir.coreRate > 0.0F) {
                change = diff * coreTempDir.coreRate;
            }
            else {
                change = diff * 0.1F;
            }
            if (this.skinTemp < this.coreTemp) {
                this.coreTemp -= change;
                if (coreTempDir == TemperatureDirection.COOLING_RAPIDLY) {
                    this.coreTemp = Math.max(this.coreTemp, NORMAL);
                }
            } else {
                this.coreTemp += change;
                if (coreTempDir == TemperatureDirection.WARMING_RAPIDLY) {
                    this.coreTemp = Math.min(this.coreTemp, NORMAL);
                }
            }
            UpdateTemperature.send(serverPlayer.getServer(),serverPlayer,this.coreTemp,this.skinTemp,(float)this.wbgt,this.thirst,sweat);
        }
    }

    private static double mcTempToCelsius(float temp) {
        double out = 25.27027027 + (44.86486486 * temp);
        out = (out - 32) * 0.5556;
        return out;
    }

    public static double mcTempConv(float temp) {
        return 25.27027027 + (44.86486486 * temp);
    }

    private static double tempToCelsius(double temp) {
        double out = (temp / 0.5556) + 32;
        return (out - 25.27027027) / 44.86486486;
    }

    private static double tempToF(double temp) {
        return (temp - 25.27027027) / 44.86486486;
    }

    private static double getBlackGlobe(double radiation, float dryTemp, double relativeHumidity) {
        double dryTempC = mcTempToCelsius(dryTemp);

        double blackGlobeTemp = (0.01498 * radiation) + (1.184 * dryTempC) - (0.0789 * (relativeHumidity / 100)) - 2.739;

        return tempToCelsius(blackGlobeTemp);
    }

    private float getBiomeTemperature(RegistryEntry<Biome> biome) {
        for(Map.Entry<TagKey<Biome>,Float> entry : temperature.entrySet()) {
            if (biome.isIn(entry.getKey())) {
                return entry.getValue();
            }
        }
        return 0.663F;
    }

    private float getBiomeHumidity(RegistryEntry<Biome> biome) {
        float rainBonus = ((serverPlayer.getServerWorld().hasRain(serverPlayer.getBlockPos().withY(320)) || serverPlayer.getServerWorld().isRaining())?0F:-20F); // We put both conditions to retain compatibility with Enhanced Weather
        for(Map.Entry<TagKey<Biome>,Float> entry : humidity.entrySet()) {
            if (biome.isIn(entry.getKey())) {
                return entry.getValue()+rainBonus;
            }
        }
        return 40.0F+rainBonus;
    }

    private float getBiomeDayNightOffset(RegistryEntry<Biome> biome) {
        for(Map.Entry<TagKey<Biome>,Float> entry : dayNightOffset.entrySet()) {
            if (biome.isIn(entry.getKey())) {
                return entry.getValue();
            }
        }
        return 0F;
    }

    private static double getHeatIndex(float dryTemp, double rh) {
        double dryTempF = mcTempConv(dryTemp);
        double hIndex;

        if (dryTempF < 80.0) {
            hIndex = 0.5 * (dryTempF + 61.0 +((dryTempF - 68.0) * 1.2)) + (rh*0.094);
        }
        else {
            hIndex = -42.379 + 2.04901523 * dryTempF + 10.14333127 * rh;
            hIndex = hIndex - 0.22475541 * dryTempF * rh - 6.83783 * Math.pow(10, -3) * dryTempF * dryTempF;
            hIndex = hIndex - 5.481717 * Math.pow(10, -2) * rh * rh;
            hIndex = hIndex + 1.22874 * Math.pow(10, -3) * dryTempF * dryTempF * rh;
            hIndex = hIndex + 8.5282 * Math.pow(10, -4) * dryTempF * rh * rh;
            hIndex = hIndex - 1.99 * Math.pow(10, -6) * dryTempF * dryTempF * rh * rh;
        }

        return tempToF(hIndex);
    }

    private double getWBGT() {
        float humidity = serverPlayer.getServer().getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE)?this.getBiomeHumidity(serverPlayer.getServerWorld().getBiome(serverPlayer.getBlockPos())):0F;
        float dryTemperature = serverPlayer.getServerWorld().getBiome(serverPlayer.getBlockPos()).value().computeTemperature(serverPlayer.getBlockPos().withY(0))+getDayNightOffset(serverPlayer.getServerWorld(),getBiomeDayNightOffset(serverPlayer.getServerWorld().getBiome(serverPlayer.getBlockPos())),humidity);
        double wetTemperature = getHeatIndex(dryTemperature,humidity);
        EnvironmentData data = getInfo();
        this.envRadiation = data.getRadiation() + getSolarRadiation(serverPlayer.getServerWorld(), BlockPos.ofFloored(serverPlayer.getCameraPosVec(1.0F)));
        double blackGlobeTemp = (float)getBlackGlobe(this.envRadiation, dryTemperature, humidity);
        double airTemperature;
        if (data.isSheltered() || data.isUnderground()) {
            airTemperature = (wetTemperature * 0.7F) + (blackGlobeTemp * 0.3F);
        } else {
            airTemperature = (wetTemperature * 0.7F) + (blackGlobeTemp * 0.2F) + (dryTemperature * 0.1F);
        }
        return airTemperature;
    }

    private EnvironmentData getInfo() {
        boolean isSheltered = true; // So basically me
        boolean isUnderground = true;
        double waterBlocks = 0;
        double totalBlocks = 0;
        double radiation = 0.0;
        BlockPos pos = serverPlayer.getBlockPos();
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                if (isSheltered && (x <= 2 && x >= -2) && (z <= 2 && z >= -2)) {
                    isSheltered = !serverPlayer.getServerWorld().isSkyVisible(BlockPos.ofFloored(serverPlayer.getCameraPosVec(1.0F)).add(x, 0, z).up());
                }
                for (int y = -3; y <= 11; y++) {
                    ChunkPos chunkPos = new ChunkPos((pos.getX() + x) >> 4,(pos.getZ() + z) >> 4);
                    Chunk chunk = serverPlayer.getServerWorld().getChunk(chunkPos.getStartPos());

                    if (chunk == null) continue;
                    BlockPos blockpos = pos.add(x, y, z);
                    PalettedContainer<BlockState> palette;
                    try {
                        palette = chunk.getSection((blockpos.getY() >> 4) - chunk.getBottomSectionCoord()).getBlockStateContainer();

                    }
                    catch (Exception e) {
                        continue;
                    }
                    BlockState state = palette.get(blockpos.getX() & 15, blockpos.getY() & 15, blockpos.getZ() & 15);
                    boolean isWater = state.isOf(Blocks.WATER);
                    if (isUnderground && y >= 0 && !isWater) {
                        isUnderground = !serverPlayer.getServerWorld().isSkyVisible(BlockPos.ofFloored(serverPlayer.getCameraPosVec(1.0F)).add(x, y, z).up());
                    }
                    if ((x <= 5 && x >= -5) && (y <= 5) && (z <= 5 && z >= -5)) {
                        totalBlocks++;

                        if (isWater) {
                            waterBlocks++;
                        }
                    }
                    if (state.isAir()) continue;
                    if(y <= 3) {
                        Float rad = ThermalRadiation.radiationBlocks.get(Registries.BLOCK.getId(state.getBlock()));
                        if (rad != null) {
                            radiation += rad;
                        }
                    }
                }
            }
        }
        return new EnvironmentData(isUnderground,isSheltered,radiation);
    }

    private static float getSolarRadiation(ServerWorld world, BlockPos pos) {
        double radiation = 0.0;
        double sunlight = world.getLightLevel(LightType.SKY, pos.up()) - world.getAmbientDarkness();
        float f = world.getSkyAngleRadians(1.0F);

        if (sunlight > 0) {
            float f1 = f < (float)Math.PI ? 0.0F : ((float)Math.PI * 2F);
            f += (f1 - f) * 0.2F;
            sunlight = sunlight * MathHelper.cos(f);
        }

        radiation += sunlight * 100;

        return (float)Math.max(radiation, 0);
    }

    private static float getDayNightOffset(ServerWorld world, float maxTemp, double relativeHumidity) {
        if(maxTemp == 0F) return 0F;
        long time = (world.getTimeOfDay() % 24000);
        float increaseTemp = (maxTemp * 0.022289157F) / 10000F;
        float decreaseTemp = (maxTemp * 0.022289157F) / 14000F;
        float humidityOffset = 1.0F - (float) (relativeHumidity / 100);
        float offset;

        if (time > 23000) {
            offset = (24001 - time) * increaseTemp;
        } else if (time < 9001) {
            offset = (time + 1000) * increaseTemp;
        } else {
            offset = (maxTemp * 0.022289157F) - ((time - 9000) * decreaseTemp);
        }

        return offset * humidityOffset;
    }

    public float getAirTemperatureSkinChange(ServerPlayerEntity sp, double insulationModifier) {
        float localTemperature = (float)this.wbgt;
        float change = 0.0F;
        double localTempF = mcTempConv(localTemperature);
        double parityTempF = mcTempConv(1.108F);
        double extremeTempF = mcTempConv(2.557F);
        double minutes;
        float radiationModifier = (float) (0 / 5000) + 1.0F;
        double temp;

        if (this.skinTempDir == TemperatureDirection.NONE) return change;

        if (localTemperature < 1.108F) {
            temp = Math.min(localTempF + insulationModifier, parityTempF);

            if (Math.abs(parityTempF - temp) > 5.0) {
                minutes = 383.4897 + (12.38784 - 383.4897) / (1 + Math.pow((temp / 43.26779), 8.271186));

                change = (NORMAL - LOW) / (float) minutes;
            }
        }
        else {
            temp = Math.max(localTempF - insulationModifier, parityTempF);

            if (Math.abs(parityTempF - temp) > 5.0) {
                // It is really, really hot ... increase rapidly
                if (temp > extremeTempF) {
                    change = (float) ((temp - extremeTempF) / 50.0) * 0.0067F;
                } else {
                    minutes = 24.45765 + (599.3552 - 24.45765) / (1 + Math.pow((temp / 109.1499), 27.47623));

                    change = (HIGH - NORMAL) / (float) minutes;
                }
            }
        }

        if ((this.coreTemp < NORMAL && 0 > 0) || radiationModifier > 5.0F) {
            change = change * radiationModifier;
        }

        return change;
    }
}
