/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.model.actor.instance;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Future;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.PetDataTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import net.sf.l2j.gameserver.model.Inventory;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2PetData;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Summon;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.PcInventory;
import net.sf.l2j.gameserver.model.PetInventory;
import net.sf.l2j.gameserver.model.actor.stat.PetStat;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.serverpackets.ItemList;
import net.sf.l2j.gameserver.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.serverpackets.PetInfo;
import net.sf.l2j.gameserver.serverpackets.PetInventoryUpdate;
import net.sf.l2j.gameserver.serverpackets.PetItemList;
import net.sf.l2j.gameserver.serverpackets.PetLiveTime;
import net.sf.l2j.gameserver.serverpackets.PetStatusShow;
import net.sf.l2j.gameserver.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.serverpackets.StopMove;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.L2Item;
import net.sf.l2j.gameserver.templates.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.L2Weapon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * This class ...
 * 
 * @version $Revision: 1.15.2.10.2.16 $ $Date: 2005/04/06 16:13:40 $
 */
public class L2PetInstance extends L2Summon
{
    final static Log _log = LogFactory.getLog(L2PetInstance.class.getName());
    
    private int _curFed;
    public PetInventory _inventory;
    public final int _controlItemId;
    public boolean _respawned;
    public boolean _mountable;
    
    private Future _feedTask;
    private int _feedTime;
    protected boolean _feedMode;
   
    private int _curWeightPenalty = 0;

    private L2PetData _data;

    /** The Experience before the last Death Penalty */
	private long _expBeforeDeath = 0; 
    private static final int FOOD_ITEM_CONSUME_COUNT = 5;
    private final int CORPSE_DECAY_TIME = 1200000;
    
    public final L2PetData getPetData()
    {
        if (_data == null) 
            _data = PetDataTable.getInstance().getPetData(getTemplate().getNpcId(), getStat().getLevel());
        
        return _data;
    }
    
    public final void setPetData(L2PetData value) { _data = value; }
    
    /**
     * Manage Feeding Task.<BR><BR>
     * 
     * <B><U> Actions</U> :</B><BR>
     * <li>Feed or kill the pet depending on hunger level</li>
     * <li>If pet has food in inventory and feed level drops below 55% then consume food from inventory</li> 
     * <li>Send a broadcastStatusUpdate packet for this L2PetInstance</li><BR><BR>
     *
     */
    
    class FeedTask implements Runnable
    {
        public void run()
        {
            try
            {
                // if pet is attacking
                if (isAttackingNow())
                    // if its not already on battleFeed mode
                    if (!_feedMode)
                        startFeed(true); //switching to battle feed
                else
                    // if its on battleFeed mode
                    if (_feedMode)
                        startFeed(false); // normal feed
                
                int foodId = PetDataTable.getFoodItemId(getTemplate().getNpcId());
                if (foodId == 0) return;

                L2ItemInstance food = null;
                food = getInventory().getItemByItemId(foodId);
                
                boolean _ownerInventory = false;
                
                // search for food in owner's inventory
                if (food == null && getOwner()!=null)
                {
                	food = getOwner().getInventory().getItemByItemId(foodId);
                	_ownerInventory = true;
                }
                
                if(getCurrentFed() < (0.55 * getMaxFed()))
                {
                    if ((food != null) &&
                        ((_ownerInventory && getOwner().destroyItem("Feed", food.getObjectId(), 1, null, false))
                            || destroyItem("Feed", food.getObjectId(), 1, null, false)))
                    {
                        setCurrentFed(getCurrentFed() + (100));

                        if (_ownerInventory)
                        {
                            SystemMessage sm = new SystemMessage(SystemMessageId.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY);
                            sm.addItemName(food);
                            getOwner().sendPacket(sm);
                        }

                        if(getCurrentFed() < (0.55 * getMaxFed()))
                        {
                            SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PET_ATE_A_LITTLE_BUT_IS_STILL_HUNGRY);
                            getOwner().sendPacket(sm);
                       }
                    }
                }

                if (getCurrentFed() > FOOD_ITEM_CONSUME_COUNT)
                {
                    // eat
                    setCurrentFed(getCurrentFed() - FOOD_ITEM_CONSUME_COUNT);
                }
                else
                {
                    setCurrentFed(0);
                }
                
                if (getOwner() != null )
                {
                	if (food == null)
                	{
                		if(getCurrentFed() >= (0.25 * getMaxFed()) &&  getCurrentFed() < (0.40 * getMaxFed()))
                		{
                			SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PET_IS_VERY_HUNGRY);
                			getOwner().sendPacket(sm);
                		}
                		else
                		if(getCurrentFed() > 0 && getCurrentFed() < (0.25 * getMaxFed()))
                		{
                			SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PET_IS_VERY_HUNGRY_PLEASE_BE_CAREFULL);
                			getOwner().sendPacket(sm);
                		}
                	}
                	// send pet food bar info
                    getOwner().sendPacket(new PetLiveTime(getOwner().getPet()));
                }
                
                broadcastStatusUpdate();
                
                if(getCurrentFed() == 0)
                {
                	SystemMessage sm = new SystemMessage(SystemMessageId.STARVING_GRUMPY_AND_FED_UP_YOUR_PET_HAS_LEFT);
                	getOwner().sendPacket(sm);
                	unSummon(getOwner()); // unsummon or die?
                }
            }
            catch (Throwable e) 
        	{
            if (_log.isDebugEnabled()) 
                _log.warn("Pet [#"+getObjectId()+"] a feed task error has occurred: "+e);
        	}	
        }
    }
    
    public synchronized static L2PetInstance spawnPet(L2NpcTemplate template, L2PcInstance owner, L2ItemInstance control)
    {
    	if (L2World.getInstance().getPet(owner.getObjectId()) != null)
    		return null; // owner has a pet listed in world
    	
    	L2PetInstance pet = restore(control, template, owner);
    	// add the pet instance to world
    	if (pet != null)
    		L2World.getInstance().addPet(owner.getObjectId(), pet);
    	
    	return pet;
    }
    
    public L2PetInstance(int objectId, L2NpcTemplate template, L2PcInstance owner, L2ItemInstance control)
    {
        super(objectId, template, owner);
        super.setStat(new PetStat(this));

        _controlItemId = control.getObjectId();
        
        // Pet's initial level is supposed to be read from DB
        // Pets start at :
        // Wolf : Level 15
        // Hatchling : Level 35
        // Tested and confirmed on official servers
        // Sin-eaters are defaulted at the owner's level
        if (template.getNpcId() == 12564)
           getStat().setLevel((byte)getOwner().getLevel());
        else
           getStat().setLevel(template.getLevel());

        _inventory = new PetInventory(this);
        int npcId = template.getNpcId();
        _mountable = PetDataTable.isMountable(npcId);
    }
    
    @Override
    public PetStat getStat()
    {
    	if(super.getStat() == null || !(super.getStat() instanceof PetStat))
    		setStat(new PetStat(this));
    	return (PetStat)super.getStat();
    }
    
    @Override
    public double getLevelMod() { return (100.0 - 11 + getLevel()) / 100.0; }
    
    public boolean isRespawned() { return _respawned; }
    
    @Override
    public int getSummonType() { return 2; }
    
    @Override
    public void onAction(L2PcInstance player)
    {
        boolean isOwner = player.getObjectId() == getOwner().getObjectId();
        boolean thisIsTarget = player.getTarget() != null && player.getTarget().getObjectId() == getObjectId();
        
        if (isOwner && thisIsTarget)
        {
            if(isOwner && player != getOwner())
            {
                // update owner
                updateRefOwner(player);
            }
            player.sendPacket(new PetStatusShow(this));
            player.sendPacket(new ActionFailed());
        }
        else
        {
            if (_log.isDebugEnabled()) _log.debug("new target selected:"+getObjectId());
            player.setTarget(this);
            MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel() - getLevel());
            player.sendPacket(my);
        }
    }
    
    @Override
    public int getControlItemId() { return _controlItemId; }
    
    public L2ItemInstance getControlItem() 
    {
        return getOwner().getInventory().getItemByObjectId(_controlItemId); 
    }
    
    @Override
    public int getCurrentFed() { return _curFed; }
    
	public void setCurrentFed(int num) { _curFed = num > getMaxFed() ? getMaxFed() : num; }
    
    //public void setPvpFlag(byte pvpFlag) { _pvpFlag = pvpFlag; }
    
    @Override
    public void setPkKills(int pkKills) { _pkKills = pkKills; }
    
    /**
     * Returns the pet's currently equipped weapon instance (if any). 
     */
    @Override
    public L2ItemInstance getActiveWeaponInstance()
    {
        for (L2ItemInstance item : getInventory().getItems())
            if (item.getLocation() == L2ItemInstance.ItemLocation.PET_EQUIP &&
                    item.getItem().getType1() == L2Item.TYPE2_WEAPON)
                return item;
        
        return null;
    }
    
    /**
     * Returns the pet's currently equipped weapon (if any). 
     */
    @Override
    public L2Weapon getActiveWeaponItem()
    {
        L2ItemInstance weapon = getActiveWeaponInstance();
        
        if (weapon == null)
            return null;
        
        return (L2Weapon)weapon.getItem();
    }
    
    @Override
    public L2ItemInstance getSecondaryWeaponInstance()
    {
        // temporary? unavailable
        return null;
    }
    
    @Override
    public L2Weapon getSecondaryWeaponItem()
    {
        // temporary? unavailable
        return null;
    }
    
    @Override
    public PetInventory getInventory() 
    { 
        return _inventory; 
    }

    /**
     * Destroys item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
     * @param process : String Identifier of process triggering this action
     * @param objectId : int Item Instance identifier of the item to be destroyed
     * @param count : int Quantity of items to be destroyed
     * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    @Override
    public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage)
    {
        L2ItemInstance item = _inventory.destroyItem(process, objectId, count, getOwner(), reference);
        
        if (item == null)
        {
            if (sendMessage) 
                getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
            
            return false;
        }

        // Send Pet inventory update packet
        _inventory.updateInventory(item);
        if (sendMessage)
        {
            SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
            sm.addNumber(count);
            sm.addItemName(item);
            getOwner().sendPacket(sm);
        }
        return true;
        
    }

    /**
     * Destroy item from inventory by using its <B>itemId</B> and send a Server->Client InventoryUpdate packet to the L2PcInstance.
     * @param process : String Identifier of process triggering this action
     * @param itemId : int Item identifier of the item to be destroyed
     * @param count : int Quantity of items to be destroyed
     * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage : boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    @Override
    public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage)
    {
        L2ItemInstance item = _inventory.destroyItemByItemId(process, itemId, count, getOwner(), reference);
        
        if (item == null)
        {
            if (sendMessage) getOwner().sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
            return false;
        }

        // Send Pet inventory update packet
        _inventory.updateInventory(item);

        if (sendMessage)
        {
            SystemMessage sm = new SystemMessage(SystemMessageId.DISSAPEARED_ITEM);
            sm.addNumber(count);
            sm.addItemName(item);
            getOwner().sendPacket(sm);
        }
        
        return true;
    }

    @Override
    protected void doPickupItem(L2Object object)
    {
    	
        getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
        StopMove sm = new StopMove(getObjectId(), getX(), getY(), getZ(), getHeading());
        
        if (_log.isDebugEnabled())
            _log.debug("Pet pickup pos: "+ object.getX() + " "+object.getY()+ " "+object.getZ() );
        
        broadcastPacket(sm);
        
        if (!(object instanceof L2ItemInstance))
        {
            // dont try to pickup anything that is not an item :)
            _log.warn("trying to pickup wrong target."+object);
            getOwner().sendPacket(new ActionFailed());
            return;
       }
       
        L2ItemInstance target = (L2ItemInstance) object;

       // Herbs
        if ( target.getItemId() > 8599 && target.getItemId() < 8615 )
       {
           SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
           smsg.addItemName(target);
           getOwner().sendPacket(smsg);
           return;
        }
        
       // Cursed weapons
       if ( CursedWeaponsManager.getInstance().isCursed(target.getItemId()) )
       {
           SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
            smsg.addItemName(target);
            getOwner().sendPacket(smsg);
           return;
       }

       int weight=ItemTable.getInstance().getTemplate(target.getItemId()).getWeight() * target.getCount();
       
       if (weight > Integer.MAX_VALUE || weight < 0 || !getInventory().validateWeight(weight))
       {
           sendPacket(new SystemMessage(SystemMessageId.YOUR_PET_CANNOT_CARRY_ANY_MORE_ITEMS));
           return;
       }

        synchronized (target)
        {
            if (!target.isVisible())
            {
                getOwner().sendPacket(new ActionFailed());
                return;
            }
            
            if (target.getOwnerId() != 0 && target.getOwnerId() != getOwner().getObjectId() && !getOwner().isInLooterParty(target.getOwnerId()))
            {
                getOwner().sendPacket(new ActionFailed());
                
                if (target.getItemId() == 57)
                {
                    SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA);
                    smsg.addNumber(target.getCount());
                    getOwner().sendPacket(smsg);
                }
                else if (target.getCount() > 1)
                {
                    SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S);
                    smsg.addItemName(target);
                    smsg.addNumber(target.getCount());
                    getOwner().sendPacket(smsg);
                }
                else
                {
                    SystemMessage smsg = new SystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1);
                    smsg.addItemName(target);
                    getOwner().sendPacket(smsg);
                }
                
                return;
            }
            if(target.getItemLootShedule() != null
                   && (target.getOwnerId() == getOwner().getObjectId() || getOwner().isInLooterParty(target.getOwnerId())))
               target.resetOwnerTimer();

            target.pickupMe(this);
            
            if(Config.SAVE_DROPPED_ITEM) // item must be removed from ItemsOnGroundManager if is active
                ItemsOnGroundManager.getInstance().removeObject(target);
        }

        getInventory().addItem("Pickup", target, getOwner(), this);
        PetItemList iu = new PetItemList(this);
        getOwner().sendPacket(iu);
        
        getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
        
        if (getFollowStatus())
            followOwner();
    }

    @Override
    public void deleteMe(L2PcInstance owner)
    {
        SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PETS_CORPSE_HAS_DECAYED);
        getOwner().sendPacket(sm);
        sm = null;
        
        super.deleteMe(owner);
        destroyControlItem(owner); //this should also delete the pet from the db
    }
    
    @Override
    public boolean doDie(L2Character killer)
    {
        if (!super.doDie(killer,true))
            return false;

        SystemMessage sm = new SystemMessage(SystemMessageId.MAKE_SURE_YOU_RESSURECT_YOUR_PET_WITHIN_20_MINUTES);
        getOwner().sendPacket(sm);

        stopFeed();
        getStatus().stopHpMpRegeneration();
        DecayTaskManager.getInstance().addDecayTask(this,CORPSE_DECAY_TIME);
        if (isRespawned()) deathPenalty();
        return true;
    }
    
    @Override
    public void doRevive()
    {
        if (getCurrentFed() > (getMaxFed() / 10))
            setCurrentFed(getMaxFed() / 10);

        getOwner().removeReviving();
        
        super.doRevive();
        super.stopDecay();
        startFeed(false);
    }
    
    @Override
    public void doRevive(double revivePower)
    {
        // Restore the pet's lost experience, 
        // depending on the % return of the skill used (based on its power).
        restoreExp(revivePower);
        doRevive();
    }

    /**
     * Transfers item to another inventory
     * @param process : String Identifier of process triggering this action
     * @param itemId : int Item Identifier of the item to be transfered
     * @param count : int Quantity of items to be transfered
     * @param actor : L2PcInstance Player requesting the item transfer
     * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the new item or the updated item in inventory
     */
    public L2ItemInstance transferItem(String process, int objectId, int count, Inventory target, L2PcInstance actor, L2Object reference)
    {
        L2ItemInstance oldItem = getInventory().getItemByObjectId(objectId); 
        L2ItemInstance newItem = getInventory().transferItem(process, objectId, count, target, actor, reference);

        if (newItem == null) return null;

        // Send inventory update packet
        PetInventoryUpdate petIU = new PetInventoryUpdate();
        if (oldItem.getCount() > 0 && oldItem != newItem) petIU.addModifiedItem(oldItem);
        else petIU.addRemovedItem(oldItem);
        getOwner().sendPacket(petIU);

        // Send target update packet
        if (target instanceof PcInventory)
        {
            L2PcInstance targetPlayer = ((PcInventory)target).getOwner();
            InventoryUpdate playerUI = new InventoryUpdate();
            if (newItem.getCount() > count) playerUI.addModifiedItem(newItem);
            else playerUI.addNewItem(newItem);
            targetPlayer.sendPacket(playerUI);

            // Update current load as well
            StatusUpdate playerSU = new StatusUpdate(targetPlayer.getObjectId());
            playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
            targetPlayer.sendPacket(playerSU);
        }
        else if (target instanceof PetInventory)
        {
            petIU = new PetInventoryUpdate();
            if (newItem.getCount() > count) petIU.addRemovedItem(newItem);
            else petIU.addNewItem(newItem);
            ((PetInventory)target).getOwner().getOwner().sendPacket(petIU);
        }
        return newItem;
    }

    @Override
    public void giveAllToOwner()
    {
        try
        {
            Inventory petInventory = getInventory();
            L2ItemInstance[] items = petInventory.getItems();
            for (int i = 0; (i < items.length); i++)
            { 
                L2ItemInstance giveit = items[i];
                if (((giveit.getItem().getWeight() * giveit.getCount())
                        + getOwner().getInventory().getTotalWeight()) 
                        < getOwner().getMaxLoad())
                {
                    // If the owner can carry it give it to them
                    giveItemToOwner(giveit);
                }
                else 
                {
                    // If they can't carry it, chuck it on the floor :)
                    dropItemHere(giveit);
                }
            }
        }
        catch(Exception e)
        {
            _log.warn("Give all items error " + e);
        }
    }
    
    public void giveItemToOwner(L2ItemInstance item)
    {
        try
        {
            getInventory().transferItem("PetTransfer", item.getObjectId(), item.getCount(), getOwner().getInventory(), getOwner(), this);   
            PetInventoryUpdate petiu = new PetInventoryUpdate();
            ItemList PlayerUI = new ItemList(getOwner(), false);
            petiu.addRemovedItem(item);
            getOwner().sendPacket(petiu);
            getOwner().sendPacket(PlayerUI);    
        }
        catch (Exception e)
        {
            _log.warn("Error while giving item to owner: " + e);
        }
    }
    
    /**
     * Remove the Pet from DB and its associated item from the player inventory
     * @param owner The owner from whose invenory we should delete the item
     */
    public void destroyControlItem(L2PcInstance owner)
    {
        // remove the pet instance from world
        L2World.getInstance().removePet(owner.getObjectId());

        // delete from inventory
        try 
        {
            L2ItemInstance removedItem = owner.getInventory().destroyItem("PetDestroy", getControlItemId(), 1, getOwner(), this);
            
            InventoryUpdate iu = new InventoryUpdate();
            iu.addRemovedItem(removedItem);
            
            owner.sendPacket(iu);
            
            StatusUpdate su = new StatusUpdate(owner.getObjectId());
            su.addAttribute(StatusUpdate.CUR_LOAD, owner.getCurrentLoad());
            owner.sendPacket(su);
            
            owner.broadcastUserInfo();
            
            L2World world = L2World.getInstance();
            world.removeObject(removedItem);
        }
        catch (Exception e){
            _log.warn("Error while destroying control item: " + e);
        }
        
        // pet control item no longer exists, delete the pet from the db
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection(con);
            PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
            statement.setInt(1, getControlItemId());
            statement.execute();
            statement.close();
        }
        catch (SQLException e)
        {
            _log.warn("could not delete pet:"+e);
        }
        finally
        {
            try { con.close(); } catch (Exception e) {}
        }
    }
    
    public void dropItemHere(L2ItemInstance dropit)
    {
        dropit = getInventory().dropItem("PetDrop", dropit.getObjectId(), dropit.getCount(), getOwner(), this);
        
        if (dropit != null)
        {
            _log.debug("Item id to drop: "+dropit.getItemId()+" amount: "+dropit.getCount());
            dropit.dropMe(this, getX(), getY(), getZ()+100);
        }
    }
    
    /**
     * Return the max weight limit of the L2PetInstance.
     */
    @Override
    public int getMaxLoad()
    {
        return getPetData().getPetMaxLoad();
    }
    
    /**
     * Return the current weight of the L2PetInstance.
     */
    @Override
    public int getCurrentLoad()
    {
        return _inventory.getTotalWeight();
    }

    /**
     * Update the overloaded status of the L2PetInstance.
     */
    public void refreshOverloaded()
    {
        getOwner().sendPacket(new PetInfo(getOwner().getPet()));
        broadcastStatusUpdate();
        
        if (getMaxLoad() > 0 && !getOwner().getDietMode())
        {
        	setIsOverloaded(getCurrentLoad() > getMaxLoad());
            int weightproc = getCurrentLoad() * 1000 / getMaxLoad();
            int newWeightPenalty;
            if (weightproc < 500) newWeightPenalty = 0;
            else if (weightproc < 666) newWeightPenalty = 1;
            else if (weightproc < 800) newWeightPenalty = 2;
            else if (weightproc < 1000) newWeightPenalty = 3;
            else newWeightPenalty = 4;

            if (_curWeightPenalty != newWeightPenalty)
            {
                _curWeightPenalty = newWeightPenalty;
                //TODO: weight penalty for pets
            }
        }
    }
     
    /** @return Returns the mountable. */
    @Override
    public boolean isMountable() { return _mountable; }

    private static L2PetInstance restore(L2ItemInstance control, L2NpcTemplate template, L2PcInstance owner)
    {
        java.sql.Connection con = null;
        try
        {
			L2PetInstance pet;
			if (template.getType().compareToIgnoreCase("L2BabyPet")==0)
				pet = new L2BabyPetInstance(IdFactory.getInstance().getNextId(), template, owner, control);
			else
				pet = new L2PetInstance(IdFactory.getInstance().getNextId(), template, owner, control);

            con = L2DatabaseFactory.getInstance().getConnection(con);
            PreparedStatement statement = con.prepareStatement("SELECT item_obj_id, name, level, curHp, curMp, exp, sp, karma, pkkills, fed FROM pets WHERE item_obj_id=?");
            statement.setInt(1, control.getObjectId());
            ResultSet rset = statement.executeQuery();
			if (!rset.next())
			{
	            rset.close();
				statement.close();
				return pet;
			}

            pet.setName(rset.getString("name"));

            pet.getStat().setLevel(rset.getByte("level"));
            pet.getStat().setExp(rset.getLong("exp"));
            pet.getStat().setSp(rset.getInt("sp"));
            pet.setKarma(rset.getInt("karma"));
            pet.setPkKills(rset.getInt("pkkills"));
            
            double curHp = rset.getDouble("curHp");
            double curMp = rset.getDouble("curMp");
            int curFed = rset.getInt("fed");
            
            if(curHp == 0)
            {
                pet.doDie(null);
                curMp = 0;
            }

           	pet.getStatus().setCurrentHp(curHp);
            pet.getStatus().setCurrentMp(curMp);
            pet.getStatus().setCurrentCp(pet.getMaxCp());    

            // hack for zero food
        	if (curFed == 0)
        	{
        		int foodId = PetDataTable.getFoodItemId(pet.getTemplate().getNpcId());
                if (foodId != 0)
                {
                	L2ItemInstance food  = pet.getOwner().getInventory().getItemByItemId(foodId);
                	
            		if ((food != null) &&  pet.getOwner().destroyItem("Feed", food.getObjectId(), 1, null, false))
            			curFed = pet.getCurrentFed() + (100);
            		else
            		{
            			pet.getOwner().sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_RESTORE_HUNGRY_PETS));
            			
                        rset.close();
                        statement.close();
                        
                        return null;
            		}
                }
        	}
        	
            pet.setCurrentFed(curFed);
            
            rset.close();
            statement.close();

            pet._respawned = true;
            
            return pet;
        } catch (SQLException e) {
            _log.warn("could not restore pet data: "+ e);
            return null;
        } finally {
            try { con.close(); } catch (Exception e) {}
        }
    }
   
    @Override
    public void store()
    {
        if (getControlItemId() == 0)
        {
            // this is a summon, not a pet, don't store anything
            return;
        }
        
        String req;
        if (!isRespawned())
            req = "INSERT INTO pets (name,level,curHp,curMp,exp,sp,karma,pkkills,fed,item_obj_id) "+
                "VALUES (?,?,?,?,?,?,?,?,?,?)";
        else
            req = "UPDATE pets SET name=?,level=?,curHp=?,curMp=?,exp=?,sp=?,karma=?,pkkills=?,fed=? "+
                "WHERE item_obj_id = ?";
        java.sql.Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection(con);
            PreparedStatement statement = con.prepareStatement(req);
            statement.setString(1, getName());
            statement.setInt(2, getStat().getLevel());
            statement.setDouble(3, getStatus().getCurrentHp());
            statement.setDouble(4, getStatus().getCurrentMp());
			statement.setLong(5, getStat().getExp());
            statement.setInt(6, getStat().getSp());
            statement.setInt(7, getKarma());
            statement.setInt(8, getPkKills());
            statement.setInt(9, getCurrentFed());
            statement.setInt(10, getControlItemId());
            statement.executeUpdate();
            statement.close();
            
            _respawned = true;
            
        } catch (SQLException e) {
            _log.warn("could not store pet data: "+e);
        } finally {
            try { con.close(); } catch (Exception e) {}
        }
        
        L2ItemInstance itemInst = getControlItem();
        if (itemInst != null && itemInst.getEnchantLevel() != getStat().getLevel())
        {
            itemInst.setEnchantLevel(getStat().getLevel());
            itemInst.updateDatabase();
        }
    }
    
    public synchronized void stopFeed()
    {
        if (_feedTask != null)
        {
            _feedTask.cancel(false);
            _feedTask = null;
            if (_log.isDebugEnabled()) _log.debug("Pet [#"+getObjectId()+"] feed task stop");
        }
    }
    
    public synchronized void startFeed( boolean battleFeed )
    {
        // stop feeding task if its active
       
        stopFeed();
        if (!isDead())
        {
            if (battleFeed)
            {
                _feedMode = true;
                _feedTime = _data.getPetFeedBattle();
            }
            else
            {
                _feedMode = false;
                _feedTime = _data.getPetFeedNormal();
            }
            //  pet feed time must be different than 0. Changing time to bypass divide by 0
            if (_feedTime <= 0) { _feedTime = 1; }
            
            int _foodConsumeTime = Math.round((60000/_feedTime)/Config.PET_FOOD_RATE);
            
            _feedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), _foodConsumeTime, _foodConsumeTime);
        }
    }
    
    @Override
    public synchronized void unSummon(L2PcInstance owner)
    {
        stopFeed();
        getStatus().stopHpMpRegeneration();
        super.unSummon(owner);
        
		if(!isDead()){
			store();
            L2World.getInstance().removePet(owner.getObjectId());
		}
    }

    /**
     * Restore the specified % of experience this L2PetInstance has lost.<BR><BR>
     */
    protected void restoreExp(double restorePercent)
    { 
        if (_expBeforeDeath > 0)
        {   
            // Restore the specified % of lost experience.
			getStat().addExp(Math.round((_expBeforeDeath - getStat().getExp()) * restorePercent / 100));
            _expBeforeDeath = 0;
        }
    }
    

    private void deathPenalty()
    {
        //FIXME: Need Correct Penalty

        int lvl = getStat().getLevel();
        double percentLost = -0.07 * lvl + 6.5;
        
        // Calculate the Experience loss
        long lostExp = Math.round((getStat().getExpForLevel(lvl+1) - getStat().getExpForLevel(lvl)) * percentLost /100);

        // Get the Experience before applying penalty
        _expBeforeDeath = getStat().getExp();
        
        // Set the new Experience value of the L2PetInstance
        getStat().addExp(-lostExp);
    }

    @Override
    public void addExpAndSp(long addToExp, int addToSp) 
    {
        if (getNpcId() == 12564) //SinEater
            getStat().addExpAndSp(Math.round(addToExp * Config.SINEATER_XP_RATE), addToSp);
        else
            getStat().addExpAndSp(Math.round(addToExp * Config.PET_XP_RATE), addToSp);
    }
    
    @Override
    public int getMaxFed() { return getStat().getMaxFeed(); }
    @Override
    public final int getLevel() { return getStat().getLevel(); }
    @Override
    public long getExpForThisLevel() { return getStat().getExpForLevel(getLevel()); }
    @Override
    public long getExpForNextLevel() { return getStat().getExpForLevel(getLevel() + 1); }
    @Override
    public int getAccuracy() { return getStat().getAccuracy(); }
    @Override
    public int getCriticalHit(L2Character target, L2Skill skill) { return getStat().getCriticalHit(target, skill); }
    @Override
    public int getEvasionRate(L2Character target) { return getStat().getEvasionRate(target); }
    @Override
    public int getRunSpeed() { return getStat().getRunSpeed(); }
    @Override
    public int getPAtkSpd() { return getStat().getPAtkSpd(); }
    @Override
    public int getMAtkSpd() { return getStat().getMAtkSpd(); }
    @Override
    public int getMAtk(L2Character target, L2Skill skill) { return getStat().getMAtk(target, skill); }
    @Override
    public int getMDef(L2Character target, L2Skill skill) { return getStat().getMDef(target, skill); }
    @Override
    public int getPAtk(L2Character target) { return getStat().getPAtk(target); }
    @Override
    public int getPDef(L2Character target) { return getStat().getPDef(target);}

    @Override
    public int getSkillLevel(int skillId)
    {
        if (_skills == null || _skills.get(skillId) == null) return -1;
        int lvl = getLevel();
        return lvl > 70 ? 7 + (lvl - 70) / 5 : lvl / 10;
    }
    
    public void updateRefOwner(L2PcInstance owner)
    {
        int oldOwnerId = getOwner().getObjectId();
        
        setOwner(owner);
        L2World.getInstance().removePet(oldOwnerId);
        L2World.getInstance().addPet(oldOwnerId, this);
    }
    
    @Override
    public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
    {
    	if (miss) return;
        	
    	// Prevents the double spam of system messages, if the target is the owning player.
    	if (target.getObjectId() != getOwner().getObjectId())
    	{
    		if (pcrit || mcrit)
    			getOwner().sendPacket(new SystemMessage(SystemMessageId.CRITICAL_HIT_BY_PET));

    		SystemMessage sm = new SystemMessage(SystemMessageId.PET_HIT_FOR_S1_DAMAGE);
    		sm.addNumber(damage);
    		getOwner().sendPacket(sm);
        }
    }    
}
