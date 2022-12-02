package edebe.mana_box.common;

import com.google.common.base.Predicates;
import com.mojang.blaze3d.matrix.MatrixStack;
import edebe.mana_box.ManaBox;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import vazkii.botania.api.BotaniaAPIClient;
import vazkii.botania.api.internal.VanillaPacketDispatcher;
import vazkii.botania.api.mana.IKeyLocked;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IThrottledPacket;
import vazkii.botania.api.mana.ManaNetworkEvent;
import vazkii.botania.api.mana.spark.ISparkAttachable;
import vazkii.botania.api.mana.spark.ISparkEntity;
import vazkii.botania.client.fx.SparkleParticleData;
import vazkii.botania.client.fx.WispParticleData;
import vazkii.botania.common.Botania;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.block.tile.TileMod;
import vazkii.botania.common.core.handler.ConfigHandler;
import vazkii.botania.common.core.handler.ManaNetworkHandler;
import vazkii.botania.common.core.helper.Vector3;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class TileManaBox extends TileMod implements IManaPool, IKeyLocked, ISparkAttachable, IThrottledPacket, ITickableTileEntity {
    public static final int PARTICLE_COLOR = 0x00C6FF;
    public static final int MAX_MANA = BlockItemManaBox.MAX_MANA;

    private static final String TAG_MANA = "mana";
    private static final String TAG_COLOR = "color";
    private static final String TAG_MANA_CAP = "manaCap";
    private static final String TAG_CAN_ACCEPT = "canAccept";
    private static final String TAG_CAN_SPARE = "canSpare";
    private static final String TAG_FRAGILE = "fragile";
    private static final String TAG_INPUT_KEY = "inputKey";
    private static final String TAG_OUTPUT_KEY = "outputKey";
    private static final int CRAFT_EFFECT_EVENT = 0;
    private static final int CHARGE_EFFECT_EVENT = 1;

    public DyeColor color = DyeColor.WHITE;
    private int mana;

    public int manaCap = MAX_MANA;
    private boolean canAccept = true;
    private boolean canSpare = true;
    public boolean fragile = false;

    private String inputKey = "";
    private final String outputKey = "";

    private int ticks = 0;
    private boolean sendPacket = false;

    public TileManaBox() {
        super(ManaBox.MANA_BOX_TILE_ENTITY_TYPE);
    }

    @Override
    public boolean isFull() {
        Block blockBelow = world.getBlockState(pos.down()).getBlock();
        return blockBelow != ModBlocks.manaVoid && getCurrentMana() >= manaCap;
    }

    @Override
    public void receiveMana(int mana) {
        int old = this.mana;
        this.mana = Math.max(0, Math.min(getCurrentMana() + mana, manaCap));
        if (old != this.mana) {
            markDirty();
            markDispatchable();
        }
    }

    @Override
    public void remove() {
        super.remove();
        ManaNetworkEvent.removePool(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        ManaNetworkEvent.removePool(this);
    }

    public static int calculateComparatorLevel(int mana, int max) {
        int val = (int) ((double) mana / (double) max * 15.0);
        if (mana > 0) {
            val = Math.max(val, 1);
        }
        return val;
    }

    @Override
    public boolean receiveClientEvent(int event, int param) {
        switch (event) {
            case CRAFT_EFFECT_EVENT: {
                if (world.isRemote) {
                    for (int i = 0; i < 25; i++) {
                        float red = (float) Math.random();
                        float green = (float) Math.random();
                        float blue = (float) Math.random();
                        SparkleParticleData data = SparkleParticleData.sparkle((float) Math.random(), red, green, blue, 10);
                        world.addParticle(data, pos.getX() + 0.5 + Math.random() * 0.4 - 0.2, pos.getY() + 0.75, pos.getZ() + 0.5 + Math.random() * 0.4 - 0.2, 0, 0, 0);
                    }
                }

                return true;
            }
            case CHARGE_EFFECT_EVENT: {
                if (world.isRemote) {
                    if (ConfigHandler.COMMON.chargingAnimationEnabled.get()) {
                        boolean outputting = param == 1;
                        Vector3 itemVec = Vector3.fromBlockPos(pos).add(0.5, 0.5 + Math.random() * 0.3, 0.5);
                        Vector3 tileVec = Vector3.fromBlockPos(pos).add(0.2 + Math.random() * 0.6, 0, 0.2 + Math.random() * 0.6);
                        Botania.proxy.lightningFX(outputting ? tileVec : itemVec, outputting ? itemVec : tileVec, 80, world.rand.nextLong(), 0x4400799c, 0x4400C6FF);
                    }
                }
                return true;
            }
            default:
                return super.receiveClientEvent(event, param);
        }
    }

    @Override
    public void tick() {
        if (!ManaNetworkHandler.instance.isPoolIn(this) && !isRemoved()) {
            ManaNetworkEvent.addPool(this);
        }

        if (world.isRemote) {
            double particleChance = 1F - (double) getCurrentMana() / (double) manaCap * 0.1;
            if (Math.random() > particleChance) {
                float red = (PARTICLE_COLOR >> 16 & 0xFF) / 255F;
                float green = (PARTICLE_COLOR >> 8 & 0xFF) / 255F;
                float blue = (PARTICLE_COLOR & 0xFF) / 255F;
                WispParticleData data = WispParticleData.wisp((float) Math.random() / 3F, red, green, blue, 2F);
                world.addParticle(data, pos.getX() + 0.3 + Math.random() * 0.5, pos.getY() + 0.6 + Math.random() * 0.25, pos.getZ() + Math.random(), 0, (float) Math.random() / 25F, 0);
            }
            return;
        }

        if (sendPacket && ticks % 10 == 0) {
            VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
            sendPacket = false;
        }

        ticks++;
    }

    @Override
    public void writePacketNBT(CompoundNBT cmp) {
        cmp.putInt(TAG_MANA, mana);
        cmp.putInt(TAG_COLOR, color.getId());

        cmp.putInt(TAG_MANA_CAP, manaCap);
        cmp.putBoolean(TAG_CAN_ACCEPT, canAccept);
        cmp.putBoolean(TAG_CAN_SPARE, canSpare);
        cmp.putBoolean(TAG_FRAGILE, fragile);

        cmp.putString(TAG_INPUT_KEY, inputKey);
        cmp.putString(TAG_OUTPUT_KEY, outputKey);
    }

    @Override
    public void readPacketNBT(CompoundNBT cmp) {
        mana = cmp.getInt(TAG_MANA);
        color = DyeColor.byId(cmp.getInt(TAG_COLOR));

        if (cmp.contains(TAG_MANA_CAP)) {
            manaCap = cmp.getInt(TAG_MANA_CAP);
        }
        if (cmp.contains(TAG_CAN_ACCEPT)) {
            canAccept = cmp.getBoolean(TAG_CAN_ACCEPT);
        }
        if (cmp.contains(TAG_CAN_SPARE)) {
            canSpare = cmp.getBoolean(TAG_CAN_SPARE);
        }
        fragile = cmp.getBoolean(TAG_FRAGILE);

        if (cmp.contains(TAG_INPUT_KEY)) {
            inputKey = cmp.getString(TAG_INPUT_KEY);
        }
        if (cmp.contains(TAG_OUTPUT_KEY)) {
            inputKey = cmp.getString(TAG_OUTPUT_KEY);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void renderHUD(MatrixStack matrixStack) {
        ItemStack pool = new ItemStack(getBlockState().getBlock());
        String name = pool.getDisplayName().getString();
        int color = 0x4444FF;
        BotaniaAPIClient.instance().drawSimpleManaHUD(matrixStack, color, getCurrentMana(), manaCap, name);
    }

    @Override
    public boolean canReceiveManaFromBursts() {
        return true;
    }

    @Override
    public boolean isOutputtingPower() {
        return false;
    }

    @Override
    public int getCurrentMana() {
        return mana;
    }

    @Override
    public String getInputKey() {
        return inputKey;
    }

    @Override
    public String getOutputKey() {
        return outputKey;
    }

    @Override
    public boolean canAttachSpark(ItemStack stack) {
        return true;
    }

    @Override
    public void attachSpark(ISparkEntity entity) {}

    @Override
    public ISparkEntity getAttachedSpark() {
        List<Entity> sparks = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos.up(), pos.up().add(1, 1, 1)), Predicates.instanceOf(ISparkEntity.class));
        if (sparks.size() == 1) {
            Entity e = sparks.get(0);
            return (ISparkEntity) e;
        }

        return null;
    }

    @Override
    public boolean areIncomingTranfersDone() {
        return false;
    }

    @Override
    public int getAvailableSpaceForMana() {
        int space = Math.max(0, manaCap - getCurrentMana());
        if (space > 0) {
            return space;
        } else if (world.getBlockState(pos.down()).getBlock() == ModBlocks.manaVoid) {
            return manaCap;
        } else {
            return 0;
        }
    }

    @Override
    public DyeColor getColor() {
        return color;
    }

    @Override
    public void setColor(DyeColor color) {
        this.color = color;
        world.notifyBlockUpdate(pos, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void markDispatchable() {
        sendPacket = true;
    }

    public void setMana(int mana) {
        this.mana = mana;
        world.notifyBlockUpdate(pos, getBlockState(), getBlockState(), 3);
    }
}
