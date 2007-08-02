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
package net.sf.l2j.gameserver.handler.skillhandlers;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.lib.Rnd;
import net.sf.l2j.gameserver.model.L2Character;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Skill.SkillType;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.effects.EffectCharge;
import net.sf.l2j.gameserver.templates.L2WeaponType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class ...
 * 
 * @version $Revision: 1.1.2.7.2.16 $ $Date: 2005/04/06 16:13:49 $
 */

public class Pdam implements ISkillHandler
{
    // all the items ids that this handler knowns
    private final static Log _log = LogFactory.getLog(Pdam.class.getName());

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.L2PcInstance, net.sf.l2j.gameserver.model.L2ItemInstance)
     */
    private static SkillType[] _skillIds = {SkillType.PDAM, SkillType.FATALCOUNTER
    /* SkillType.CHARGEDAM */
    };

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.handler.IItemHandler#useItem(net.sf.l2j.gameserver.model.L2PcInstance, net.sf.l2j.gameserver.model.L2ItemInstance)
     */
    public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
    {
        if (activeChar.isAlikeDead()) return;

        int damage = 0;

        if (_log.isDebugEnabled())
            _log.info("Begin Skill processing in Pdam.java " + skill.getSkillType());

        for (int index = 0; index < targets.length; index++)
        {
            L2Character target = (L2Character) targets[index];
            Formulas f = Formulas.getInstance();
            if(target.reflectSkill(skill))
               target = activeChar;            
            L2ItemInstance weapon = activeChar.getActiveWeaponInstance();
            if (activeChar instanceof L2PcInstance && target instanceof L2PcInstance
                && target.isAlikeDead() && target.isFakeDeath())
            {
                target.stopFakeDeath(null);
            }
            else if (target.isAlikeDead()) continue;

            boolean dual = activeChar.isUsingDualWeapon();
            boolean shld = f.calcShldUse(activeChar, target);
            // PDAM critical chance not affected by buffs, only by STR. Only some skills are meant to crit.
            boolean crit = false;
            if (skill.getBaseCritRate() > 0) 
            	crit = f.calcCrit(skill.getBaseCritRate() * 10 * f.getSTRBonus(activeChar)); 
            
            boolean soul = (weapon != null && weapon.getChargedSoulshot() == L2ItemInstance.CHARGED_SOULSHOT && weapon.getItemType() != L2WeaponType.DAGGER);
            
            if (skill.ignoreShld()) shld = false;

            if (!crit && (skill.getCondition() & L2Skill.COND_CRIT) != 0) damage = 0;
            else damage = (int) f.calcPhysDam(activeChar, target, skill, shld, false, dual, soul);
            if (crit) damage *= 2; // PDAM Critical damage always 2x and not affected by buffs


            if (damage > 5000 && activeChar instanceof L2PcInstance)
            {
                String name = "";
                if (target instanceof L2RaidBossInstance) name = "RaidBoss ";
                if (target instanceof L2NpcInstance)
                    name += target.getName() + "(" + ((L2NpcInstance) target).getTemplate().getNpcId()
                        + ")";
                if (target instanceof L2PcInstance)
                    name = target.getName() + "(" + target.getObjectId() + ") ";
                name += target.getLevel() + " lvl";
                if(_log.isDebugEnabled())
                    _log.info(activeChar.getName() + "(" + activeChar.getObjectId() + ") "
                        + activeChar.getLevel() + " lvl did damage " + damage + " with skill "
                        + skill.getName() + "(" + skill.getId() + ") to " + name);
            }

            if (soul && weapon != null) weapon.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
            
            if (damage > 0)
            {
            	activeChar.sendDamageMessage(target, damage, false, crit, false);

                if (skill.hasEffects())
                {
                    // activate attacked effects, if any
                    target.stopEffect(skill.getId());
                    if (target.getEffect(skill.getId()) != null)
                        target.removeEffect(target.getEffect(skill.getId()));
                    if (f.calcSkillSuccess(activeChar, target, skill, false, false, false))
                    {
                        skill.getEffects(activeChar, target);
                        
                        SystemMessage sm = new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
                        sm.addSkillName(skill.getId());
                        target.sendPacket(sm);
                    }
                    else
                    {
                    	SystemMessage sm = new SystemMessage(SystemMessageId.S1_WAS_UNAFFECTED_BY_S2);
                        sm.addString(target.getName());
                        sm.addSkillName(skill.getDisplayId());
                        activeChar.sendPacket(sm);
                    }
                }
                
                 // Success of lethal effect
                int chance = Rnd.get(100);
                if(!target.isRaid() 
                		&& chance < skill.getLethalChance1()
                		&& !(target instanceof L2DoorInstance)
    					&& !(target instanceof L2NpcInstance && ((L2NpcInstance)target).getNpcId() == 35062))
    			{                    // 1st lethal effect activate (cp to 1 or if target is npc then hp to 50%)
                	if(skill.getLethalChance2() > 0 && chance >= skill.getLethalChance2())
                    {
                       if (target instanceof L2PcInstance) 
                       {
                            L2PcInstance player = (L2PcInstance)target;
                            if (!player.isInvul())
                            {
                                player.getStatus().setCurrentCp(1); // Set CP to 1
                                player.reduceCurrentHp(damage, activeChar);
                            }
                       }
                       else if (target instanceof L2MonsterInstance) // If is a monster remove first damage and after 50% of current hp
                       {
                          target.reduceCurrentHp(damage, activeChar);
                          target.reduceCurrentHp(target.getStatus().getCurrentHp()/2, activeChar);
                       }
                    }
                    else //2nd lethal effect activate (cp,hp to 1 or if target is npc then hp to 1)
                    {
                         // If is a monster damage is (CurrentHp - 1) so HP = 1
                        if (target instanceof L2NpcInstance)
                            target.reduceCurrentHp(target.getStatus().getCurrentHp()-1, activeChar);
                        else if (target instanceof L2PcInstance) // If is a active player set his HP and CP to 1
                        {
                            L2PcInstance player = (L2PcInstance)target;
                            if (!player.isInvul())
                            {
                                player.getStatus().setCurrentHp(1);
                                player.getStatus().setCurrentCp(1);
                            }
                        }
                    }
                    // Lethal Strike was succefful!
                    activeChar.sendPacket(new SystemMessage(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL));
                }
                else
                {
                    // Make damage directly to HP
                    if(skill.getDmgDirectlyToHP())
                    {
                        if(target instanceof L2PcInstance)
                        {
                            L2PcInstance player = (L2PcInstance)target;
                            if (!player.isInvul())
                            {
                               if (damage >= player.getStatus().getCurrentHp()) 
                               {
                                   player.getStatus().setCurrentHp(0);
                                   player.doDie(activeChar);
                               }
                               else 
                                  player.getStatus().setCurrentHp(player.getStatus().getCurrentHp() - damage);
                            }
	                		
	                		SystemMessage smsg = new SystemMessage(SystemMessageId.S1_GAVE_YOU_S2_DMG);
	                		smsg.addString(activeChar.getName());
	                		smsg.addNumber(damage);
	                		player.sendPacket(smsg);
	                		
                        }
                        else
                            target.reduceCurrentHp(damage, activeChar);
                    }
                    else
                    {
                        target.reduceCurrentHp(damage, activeChar);
                    }
                }
            }
            else // No - damage
            {
                activeChar.sendPacket(new SystemMessage(SystemMessageId.ATTACK_FAILED));
            }
            if (skill.getId() == 345 || skill.getId() == 346) // Sonic Rage or Raging Force
            {
                EffectCharge effect = (EffectCharge)activeChar.getEffect(L2Effect.EffectType.CHARGE);
                if (effect != null) 
                {
                    int effectcharge = effect.getLevel();
                    if (effectcharge < 7)
                    {
                        effectcharge++;
                        effect.addNumCharges(1);
                        activeChar.updateEffectIcons();
                        SystemMessage sm = new SystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1);
                        sm.addNumber(effectcharge);
                        activeChar.sendPacket(sm);
                    }
                    else
                    {
                        SystemMessage sm = new SystemMessage(SystemMessageId.FORCE_MAXLEVEL_REACHED);
                        activeChar.sendPacket(sm);
                    }
                }
                else
                {
                    if (skill.getId() == 345) // Sonic Rage
                    {
                        L2Skill dummy = SkillTable.getInstance().getInfo(8, 7); // Lv7 Sonic Focus
                        dummy.getEffects(activeChar, activeChar);
                    }
                    else if (skill.getId() == 346) // Raging Force
                    {
                        L2Skill dummy = SkillTable.getInstance().getInfo(50, 7); // Lv7 Focused Force
                        dummy.getEffects(activeChar, activeChar);
                    }
                }
            }  
            //self Effect :]
            L2Effect effect = activeChar.getEffect(skill.getId());        
            if (effect != null && effect.isSelfEffect())        
            {            
                //Replace old effect with new one.            
                effect.exit();        
            }        
            skill.getEffectsSelf(activeChar);
        }
        
        if (skill.isSuicideAttack())
        {
            activeChar.doDie(null);
            activeChar.getStatus().setCurrentHp(0);
        }
    }

    public SkillType[] getSkillIds()
    {
        return _skillIds;
    }
}
