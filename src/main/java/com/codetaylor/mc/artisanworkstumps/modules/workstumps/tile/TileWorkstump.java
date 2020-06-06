package com.codetaylor.mc.artisanworkstumps.modules.workstumps.tile;

import com.codetaylor.mc.artisanworkstumps.ModArtisanWorkstumps;
import com.codetaylor.mc.artisanworkstumps.modules.tanks.block.BlockFluidStump;
import com.codetaylor.mc.artisanworkstumps.modules.tanks.tile.TileFluidStump;
import com.codetaylor.mc.artisanworkstumps.modules.workstumps.ModuleWorkstumps;
import com.codetaylor.mc.artisanworkstumps.modules.workstumps.ModuleWorkstumpsConfig;
import com.codetaylor.mc.artisanworkstumps.modules.workstumps.block.BlockWorkstump;
import com.codetaylor.mc.artisanworkstumps.modules.workstumps.tile.workstump.*;
import com.codetaylor.mc.artisanworktables.api.ArtisanAPI;
import com.codetaylor.mc.artisanworktables.api.internal.recipe.ISecondaryIngredientMatcher;
import com.codetaylor.mc.artisanworktables.api.internal.recipe.RecipeRegistry;
import com.codetaylor.mc.artisanworktables.api.internal.reference.EnumTier;
import com.codetaylor.mc.artisanworktables.api.recipe.IArtisanRecipe;
import com.codetaylor.mc.athenaeum.integration.gamestages.Stages;
import com.codetaylor.mc.athenaeum.interaction.spi.IInteraction;
import com.codetaylor.mc.athenaeum.interaction.spi.ITileInteractable;
import com.codetaylor.mc.athenaeum.network.tile.data.TileDataEnum;
import com.codetaylor.mc.athenaeum.network.tile.data.TileDataFloat;
import com.codetaylor.mc.athenaeum.network.tile.data.TileDataInteger;
import com.codetaylor.mc.athenaeum.network.tile.data.TileDataItemStackHandler;
import com.codetaylor.mc.athenaeum.network.tile.spi.ITileData;
import com.codetaylor.mc.athenaeum.network.tile.spi.TileEntityDataBase;
import com.codetaylor.mc.athenaeum.util.Properties;
import com.codetaylor.mc.athenaeum.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class TileWorkstump
    extends TileEntityDataBase
    implements ITileInteractable {

  private TileDataItemStackHandler<StackHandlerInput> inputTileDataItemStackHandler;
  private StackHandlerInput stackHandlerInput;
  private StackHandlerShelf stackHandlerShelf;
  private TileDataInteger remainingDurability;
  private TileDataEnum<EnumDamagedSide>[] damagedSides;

  private TileDataFloat recipeProgress;

  private IInteraction[] interactions;

  private String tableName;
  private String retainedRecipeName;

  public TileWorkstump() {

    super(ModuleWorkstumps.TILE_DATA_SERVICE);

    // --- Initialize ---

    this.stackHandlerInput = new StackHandlerInput(3, 3, this.getGridMaxStackSize());
    this.stackHandlerInput.addObserver((handler, slot) -> {
      this.recipeProgress.set(0);
      this.markDirty();
    });

    this.stackHandlerShelf = new StackHandlerShelf(this.getShelfMaxStackSize());
    this.stackHandlerShelf.addObserver((handler, slot) -> this.markDirty());

    this.recipeProgress = new TileDataFloat(0);

    this.remainingDurability = new TileDataInteger(this.getDurability());

    //noinspection unchecked
    this.damagedSides = new TileDataEnum[3];

    for (int i = 0; i < this.damagedSides.length; i++) {
      this.damagedSides[i] = new TileDataEnum<>(
          ordinal -> EnumDamagedSide.values()[ordinal],
          Enum::ordinal,
          EnumDamagedSide.None
      );
    }

    // --- Network ---

    this.inputTileDataItemStackHandler = new TileDataItemStackHandler<>(this.stackHandlerInput);

    this.registerTileDataForNetwork(new ITileData[]{
        this.inputTileDataItemStackHandler,
        new TileDataItemStackHandler<>(this.stackHandlerShelf),
        this.recipeProgress,
        this.remainingDurability,
        this.damagedSides[0],
        this.damagedSides[1],
        this.damagedSides[2]
    });

    // --- Interactions ---

    this.interactions = new IInteraction[12];

    List<IInteraction> interactionList = new ArrayList<>();

    interactionList.add(new InteractionTool());

    for (int i = 0; i < 9; i++) {
      int x = 2 - (i % 3);
      int z = 2 - (i / 3);
      interactionList.add(new InteractionInput(this::getTableName, this.stackHandlerInput, i, x, z));
    }

    for (int i = 0; i < 3; i++) {
      int x = i % 3;
      interactionList.add(new InteractionShelf(this.stackHandlerShelf, i, x));
    }

    this.interactions = interactionList.toArray(new IInteraction[0]);
  }

  public TileWorkstump(String tableName) {

    this();
    this.tableName = tableName;
  }

  // ---------------------------------------------------------------------------
  // - Accessors
  // ---------------------------------------------------------------------------

  public String getTableName() {

    return this.tableName;
  }

  public IArtisanRecipe getRetainedRecipe() {

    if (this.retainedRecipeName == null) {
      return null;
    }

    IArtisanRecipe recipe = ArtisanAPI.getRecipe(this.retainedRecipeName);

    if (recipe == null) {
      this.retainedRecipeName = null;
    }

    return recipe;
  }

  public void setRetainedRecipeName(String recipeName) {

    this.retainedRecipeName = recipeName;
  }

  public float getRecipeProgress() {

    return this.recipeProgress.get();
  }

  public void setRecipeProgress(float recipeProgress) {

    this.recipeProgress.set(recipeProgress);
  }

  public void addRecipeProgress(float recipeProgress) {

    this.recipeProgress.add(recipeProgress);
  }

  public int getRemainingDurability() {

    return this.remainingDurability.get();
  }

  public int addRemainingDurability(int value) {

    int durabilityMax = this.getDurability();
    int remainingDurability = Math.max(0, Math.min(durabilityMax, this.remainingDurability.get() + value));
    this.remainingDurability.set(remainingDurability);
    this.updateDamagedSides(remainingDurability, durabilityMax);
    return remainingDurability;
  }

  public boolean isSideDamaged(EnumDamagedSide side) {

    for (TileDataEnum<EnumDamagedSide> damagedSide : this.damagedSides) {

      if (damagedSide.get() == side) {
        return true;
      }
    }

    return false;
  }

  public StackHandlerInput getStackHandlerInput() {

    return this.stackHandlerInput;
  }

  public boolean hasFluidStump() {

    return this.getFluidStump() != null;
  }

  /**
   * @return the connected fluid stump's fluid handler or null if none
   */
  @Nullable
  public IFluidHandler getFluidHandler() {

    TileFluidStump tileEntity = this.getFluidStump();

    if (tileEntity == null) {
      return null;
    }

    return tileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
  }

  /**
   * Visits each horizontal side of the workstump with exception to the front
   * and returns the first fluid stump found that is also facing this workstump.
   *
   * @return the connected fluid stump or null if none
   */
  @Nullable
  public TileFluidStump getFluidStump() {

    for (int i = 0; i < EnumFacing.HORIZONTALS.length; i++) {
      BlockPos offset = this.getPos().offset(EnumFacing.HORIZONTALS[i]);

      IBlockState blockState = this.world.getBlockState(offset);

      if (!(blockState.getBlock() instanceof BlockFluidStump)) {
        continue;
      }

      if (blockState.getValue(Properties.FACING_HORIZONTAL) != EnumFacing.HORIZONTALS[i].getOpposite()) {
        continue;
      }

      TileEntity tileEntity = this.world.getTileEntity(offset);

      if (!(tileEntity instanceof TileFluidStump)) {
        continue;
      }

      return (TileFluidStump) tileEntity;
    }

    return null;
  }

  public IArtisanRecipe getWorkstumpRecipe(EntityPlayer player) {

    int playerExperience = EnchantmentHelper.getPlayerExperienceTotal(player);
    int playerLevels = player.experienceLevel;
    boolean isPlayerCreative = player.isCreative();

    RecipeRegistry registry = ArtisanAPI.getWorktableRecipeRegistry(this.tableName);

    FluidStack simulatedFluidStackAvailable = null;
    IFluidHandler fluidHandler = this.getFluidHandler();

    if (fluidHandler != null) {
      FluidStack simulatedFluidStackDrained = fluidHandler.drain(Integer.MAX_VALUE, false);

      if (simulatedFluidStackDrained != null && simulatedFluidStackDrained.amount > 0) {
        simulatedFluidStackAvailable = simulatedFluidStackDrained;
      }
    }

    return registry.findRecipe(
        playerExperience,
        playerLevels,
        isPlayerCreative,
        new ItemStack[]{player.getHeldItemMainhand()},
        this.stackHandlerInput,
        simulatedFluidStackAvailable,
        ISecondaryIngredientMatcher.FALSE,
        EnumTier.WORKTABLE,
        Collections.emptyMap()
    );
  }

  private int getGridMaxStackSize() {

    return ModuleWorkstumpsConfig.WORKSTUMP.GRID_MAX_STACK_SIZE;
  }

  private int getShelfMaxStackSize() {

    return ModuleWorkstumpsConfig.WORKSTUMP.SHELF_MAX_STACK_SIZE;
  }

  public boolean usesDurability() {

    return ModuleWorkstumpsConfig.WORKSTUMP.USES_DURABILITY;
  }

  public int getDurability() {

    return ModuleWorkstumpsConfig.WORKSTUMP.DURABILITY;
  }

  public int getHitsPerCraft() {

    return ModuleWorkstumpsConfig.WORKSTUMP.HITS_PER_CRAFT;
  }

  public int getMinimumHungerToUse() {

    return ModuleWorkstumpsConfig.WORKSTUMP.MINIMUM_HUNGER_TO_USE;
  }

  public double getExhaustionCostPerHit() {

    return ModuleWorkstumpsConfig.WORKSTUMP.EXHAUSTION_COST_PER_HIT;
  }

  public double getExhaustionCostPerCraftComplete() {

    return ModuleWorkstumpsConfig.WORKSTUMP.EXHAUSTION_COST_PER_CRAFT_COMPLETE;
  }

  // ---------------------------------------------------------------------------
  // - Container
  // ---------------------------------------------------------------------------

  public void dropContents() {

    StackHelper.spawnStackHandlerContentsOnTop(this.world, this.stackHandlerInput, this.pos);
    StackHelper.spawnStackHandlerContentsOnTop(this.world, this.stackHandlerShelf, this.pos);
  }

  // ---------------------------------------------------------------------------
  // - Serialization
  // ---------------------------------------------------------------------------

  @Override
  protected void setWorldCreate(World world) {

    this.world = world;
  }

  @Override
  public void readFromNBT(NBTTagCompound compound) {

    super.readFromNBT(compound);

    this.tableName = compound.getString("tableName");
    this.stackHandlerInput.deserializeNBT(compound.getCompoundTag("inputStackHandler"));
    this.stackHandlerShelf.deserializeNBT(compound.getCompoundTag("shelfStackHandler"));
    this.remainingDurability.set(compound.getInteger("remainingDurability"));

    if (compound.hasKey("retainedRecipe")) {
      this.retainedRecipeName = compound.getString("retainedRecipe");
    }

    this.damagedSides[0].set(EnumDamagedSide.values()[compound.getByte("damagedSide0")]);
    this.damagedSides[1].set(EnumDamagedSide.values()[compound.getByte("damagedSide1")]);
    this.damagedSides[2].set(EnumDamagedSide.values()[compound.getByte("damagedSide2")]);
  }

  @Nonnull
  @Override
  public NBTTagCompound writeToNBT(NBTTagCompound compound) {

    super.writeToNBT(compound);

    compound.setString("tableName", this.tableName);
    compound.setTag("inputStackHandler", this.stackHandlerInput.serializeNBT());
    compound.setTag("shelfStackHandler", this.stackHandlerShelf.serializeNBT());
    compound.setInteger("remainingDurability", this.remainingDurability.get());

    if (this.retainedRecipeName != null) {
      compound.setString("retainedRecipe", this.retainedRecipeName);
    }

    compound.setByte("damagedSide0", (byte) this.damagedSides[0].get().ordinal());
    compound.setByte("damagedSide1", (byte) this.damagedSides[1].get().ordinal());
    compound.setByte("damagedSide2", (byte) this.damagedSides[2].get().ordinal());

    return compound;
  }

  // ---------------------------------------------------------------------------
  // - Interactions
  // ---------------------------------------------------------------------------

  @Nullable
  @Override
  public Stages getStages() {

    return ModuleWorkstumpsConfig.STAGES_WORKSTUMP;
  }

  @Override
  public boolean shouldRenderInPass(int pass) {

    return (pass == 0) || (pass == 1);
  }

  @Override
  public IInteraction[] getInteractions() {

    return this.interactions;
  }

  @Override
  public EnumFacing getTileFacing(World world, BlockPos pos, IBlockState blockState) {

    if (blockState.getBlock() instanceof BlockWorkstump) {
      return blockState.getValue(Properties.FACING_HORIZONTAL);
    }

    return ITileInteractable.super.getTileFacing(world, pos, blockState);
  }

  public int getBlockStateIdForParticles() {

    IBlockState state = Blocks.PLANKS.getDefaultState()
        .withProperty(BlockPlanks.VARIANT, BlockPlanks.EnumType.OAK);
    return Block.getStateId(state);
  }

  // ---------------------------------------------------------------------------
  // - Network
  // ---------------------------------------------------------------------------

  @Override
  public void onTileDataUpdate() {

    boolean requiresBlockUpdate = false;

    if ("mage".equals(this.tableName) && this.inputTileDataItemStackHandler.isDirty()) {
      requiresBlockUpdate = true;
    }

    if (this.remainingDurability.isDirty()) {
      requiresBlockUpdate = true;
    }

    for (int i = 0; i < this.damagedSides.length; i++) {

      if (this.damagedSides[i].isDirty()) {
        requiresBlockUpdate = true;
      }
    }

    if (requiresBlockUpdate) {
      BlockHelper.notifyBlockUpdate(this.world, this.pos);
    }
  }

  // ---------------------------------------------------------------------------
  // - Internal
  // ---------------------------------------------------------------------------

  private EnumDamagedSide selectSideToDamage() {

    // Create a set of all sides.
    Set<EnumDamagedSide> sideSet = new HashSet<EnumDamagedSide>(3) {{
      add(EnumDamagedSide.West);
      add(EnumDamagedSide.East);
      add(EnumDamagedSide.South);
    }};

    // Remove any sides that are already damaged.
    for (TileDataEnum<EnumDamagedSide> damagedSide : this.damagedSides) {
      sideSet.remove(damagedSide.get());
    }

    // Select and return a random value from the remaining set.
    EnumDamagedSide[] sides = sideSet.toArray(new EnumDamagedSide[0]);
    return sides[RandomHelper.random().nextInt(sides.length)];
  }

  private void updateDamagedSides(int remainingDurability, int durabilityMax) {

    float durabilityPercentage = remainingDurability / (float) durabilityMax;

    if (durabilityPercentage <= 0.11) {
      // should have 3 panels down

      for (TileDataEnum<EnumDamagedSide> damagedSide : this.damagedSides) {

        if (damagedSide.get() == EnumDamagedSide.None) {
          damagedSide.set(this.selectSideToDamage());
        }
      }

    } else if (durabilityPercentage <= 0.22) {
      // should have 2 panels down

      if (this.damagedSides[2].get() != EnumDamagedSide.None) {
        this.damagedSides[2].set(EnumDamagedSide.None);
      }

      for (int i = 0; i < this.damagedSides.length - 1; i++) {

        if (this.damagedSides[i].get() == EnumDamagedSide.None) {
          this.damagedSides[i].set(this.selectSideToDamage());
        }
      }

    } else if (durabilityPercentage <= 0.33) {
      // should have 1 panel down

      for (int i = 1; i < this.damagedSides.length; i++) {

        if (this.damagedSides[i].get() != EnumDamagedSide.None) {
          this.damagedSides[i].set(EnumDamagedSide.None);
        }
      }

      if (this.damagedSides[0].get() == EnumDamagedSide.None) {
        this.damagedSides[0].set(this.selectSideToDamage());
      }

    } else {
      // should have 0 panels down

      for (TileDataEnum<EnumDamagedSide> damagedSide : this.damagedSides) {

        if (damagedSide.get() != EnumDamagedSide.None) {
          damagedSide.set(EnumDamagedSide.None);
        }
      }
    }

    // Break the connected fluid stump if it is attached to a damaged side.

    TileFluidStump fluidStump = this.getFluidStump();

    if (fluidStump == null) {
      return;
    }

    BlockPos fluidStumpPos = fluidStump.getPos();
    IBlockState fluidStumpBlockState = this.world.getBlockState(fluidStumpPos);
    EnumFacing fluidStumpFacing = fluidStumpBlockState.getValue(Properties.FACING_HORIZONTAL);
    EnumFacing workstumpFacing = fluidStumpFacing.getOpposite();

    if (this.isSideDamaged(workstumpFacing)) {
      this.world.destroyBlock(fluidStumpPos, true);
    }
  }

  public boolean isSideDamaged(EnumFacing side) {

    IBlockState blockState = this.world.getBlockState(this.pos);

    if (!(blockState.getBlock() instanceof BlockWorkstump)) {
      return false;
    }

    EnumFacing facing = blockState.getValue(Properties.FACING_HORIZONTAL);

    if (facing == side) {
      return false;
    }

    // Translate to local block facing.
    EnumFacing localFacing;

    switch (facing) {
      case SOUTH:
        localFacing = side.rotateY().rotateY();
        break;
      case EAST:
        localFacing = side.rotateYCCW();
        break;
      case WEST:
        localFacing = side.rotateY();
        break;
      case NORTH:
      default:
        localFacing = side;
        break;
    }

    // Translate to damaged side enum.
    EnumDamagedSide damagedSide;

    switch (localFacing) {
      case EAST:
        damagedSide = EnumDamagedSide.East;
        break;
      case WEST:
        damagedSide = EnumDamagedSide.West;
        break;
      case SOUTH:
        damagedSide = EnumDamagedSide.South;
        break;
      default:
        ModArtisanWorkstumps.LOGGER.error("Error translating local facing: " + localFacing.toString());
        return false;
    }

    // Check if the translated side is damaged and disallow if it is.
    return this.isSideDamaged(damagedSide);
  }
}
