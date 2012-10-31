package de.kumpelblase2.remoteentities.entities;

import java.lang.reflect.Field;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.EntitySheep;
import net.minecraft.server.Item;
import net.minecraft.server.World;
import de.kumpelblase2.remoteentities.api.RemoteEntity;
import de.kumpelblase2.remoteentities.api.RemoteEntityHandle;
import de.kumpelblase2.remoteentities.api.events.RemoteEntityTouchEvent;
import de.kumpelblase2.remoteentities.api.features.InventoryFeature;
import de.kumpelblase2.remoteentities.api.thinking.InteractBehaviour;
import de.kumpelblase2.remoteentities.api.thinking.Mind;
import de.kumpelblase2.remoteentities.api.thinking.PathfinderGoalSelectorHelper;
import de.kumpelblase2.remoteentities.api.thinking.TouchBehaviour;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireAttackOnCollide;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireAttackTarget;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireBegForItem;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireBreed;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireEatGrass;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireFollowParent;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireFollowTamer;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireHelpAttacking;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireLeapAtTarget;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireLookAtNearest;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireLookRandomly;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireNonTamedAttackNearest;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesirePanic;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireProtectOwner;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireSit;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireSwim;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireTempt;
import de.kumpelblase2.remoteentities.api.thinking.goals.DesireWanderAround;
import de.kumpelblase2.remoteentities.utilities.ReflectionUtil;

public class RemoteSheepEntity extends EntitySheep implements RemoteEntityHandle
{
	private RemoteEntity m_remoteEntity;
	protected final PathfinderGoalSelectorHelper goalSelectorHelper;
	protected final PathfinderGoalSelectorHelper targetSelectorHelper;
	protected int m_maxHealth;
	public static int defaultMaxHealth = 8;
	protected int m_lastBouncedId;
	protected long m_lastBouncedTime;
	
	static
	{
		ReflectionUtil.registerEntityType(RemoteSheepEntity.class, "Sheep", 91);
	}
	
	public RemoteSheepEntity(World world)
	{
		this(world, null);
	}
	
	public RemoteSheepEntity(World world, RemoteEntity inRemoteEntity)
	{
		super(world);
		this.m_remoteEntity = inRemoteEntity;
		this.goalSelectorHelper = new PathfinderGoalSelectorHelper(this.goalSelector);
		this.targetSelectorHelper = new PathfinderGoalSelectorHelper(this.targetSelector);
		this.m_maxHealth = defaultMaxHealth;
	}
	
	@Override
	public Inventory getInventory()
	{
		if(!this.m_remoteEntity.getFeatures().hasFeature("Inventory"))
			return null;
		
		return ((InventoryFeature)this.m_remoteEntity.getFeatures().getFeature("Inventory")).getInventory();
	}

	@Override
	public RemoteEntity getRemoteEntity()
	{
		return this.m_remoteEntity;
	}

	@Override
	public void setupStandardGoals()
	{
		try
		{
			Mind mind = this.getRemoteEntity().getMind();
			mind.addMovementDesire(new DesireSwim(this.getRemoteEntity()), 0);
			mind.addMovementDesire(new DesirePanic(this.getRemoteEntity()), 1);
			mind.addMovementDesire(new DesireBreed(this.getRemoteEntity()), 2);
			mind.addMovementDesire(new DesireTempt(this.getRemoteEntity(), Item.WHEAT.id, false), 3);
			mind.addMovementDesire(new DesireFollowParent(this.getRemoteEntity()), 4);
			mind.addMovementDesire(new DesireEatGrass(this.getRemoteEntity()), 5);
			mind.addMovementDesire(new DesireWanderAround(this.getRemoteEntity()), 6);
			mind.addMovementDesire(new DesireLookAtNearest(this.getRemoteEntity(), EntityHuman.class, 6F), 7);
			mind.addMovementDesire(new DesireLookRandomly(this.getRemoteEntity()), 8);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public PathfinderGoalSelectorHelper getGoalSelector()
	{
		return this.goalSelectorHelper;
	}

	@Override
	public PathfinderGoalSelectorHelper getTargetSelector()
	{
		return this.targetSelectorHelper;
	}

	@Override
	public void setMaxHealth(int inHealth)
	{
		this.m_maxHealth = inHealth;
	}
	
	@Override
	public int getMaxHealth()
	{
		if(this.m_maxHealth == 0)
			return defaultMaxHealth;
		return this.m_maxHealth;
	}
	
	@Override
	public void h_()
	{
		super.h_();
		this.getRemoteEntity().getMind().tick();
	}
	
	@Override
	public void b_(EntityHuman entity)
	{
		if(entity instanceof EntityPlayer)
		{
			if (this.getRemoteEntity().getMind().canFeel() && (this.m_lastBouncedId != entity.id || System.currentTimeMillis() - this.m_lastBouncedTime > 1000) && this.getRemoteEntity().getMind().hasBehaviour("Touch")) {
				if(entity.getBukkitEntity().getLocation().distanceSquared(getBukkitEntity().getLocation()) <= 1)
				{
					RemoteEntityTouchEvent event = new RemoteEntityTouchEvent(this.m_remoteEntity, entity.getBukkitEntity());
					Bukkit.getPluginManager().callEvent(event);
					if(event.isCancelled())
						return;
					
					((TouchBehaviour)this.getRemoteEntity().getMind().getBehaviour("Touch")).onTouch((Player)entity.getBukkitEntity());
					this.m_lastBouncedTime = System.currentTimeMillis();
					this.m_lastBouncedId = entity.id;
				}
			}
		}
		super.b_(entity);
	}
	
	@Override
	public boolean c(EntityHuman entity)
	{
		if(entity instanceof EntityPlayer && this.getRemoteEntity().getMind().canFeel() && this.getRemoteEntity().getMind().hasBehaviour("Interact"))
		{
			((InteractBehaviour)this.getRemoteEntity().getMind().getBehaviour("Interact")).onInteract((Player)entity.getBukkitEntity());
		}
		
		return super.c(entity);
	}
	
	
	@Override
	protected void bc()
	{
		try
		{
			Field tickField = EntitySheep.class.getDeclaredField("e");
			tickField.setAccessible(true);
			tickField.set(this, this.getRemoteEntity().getMind().getActionDesire(DesireEatGrass.class).tickTime());
			super.bc();
		}
		catch(Exception e)
		{
		}
	}
}