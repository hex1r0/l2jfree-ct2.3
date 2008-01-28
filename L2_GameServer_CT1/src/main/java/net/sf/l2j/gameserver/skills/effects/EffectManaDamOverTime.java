/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Env;

class EffectManaDamOverTime extends L2Effect
{
	public EffectManaDamOverTime(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	public EffectType getEffectType()
	{
		return EffectType.MANA_DMG_OVER_TIME;
	}

	public boolean onActionTime()
	{
		if (getEffected().isDead())
			return false;

		if (getSkill().getId() == 5127) /* "Recover Force" is the Fusion Skill */
		{
			/*
			 * If the target has the chargers 50 (Focused Force) or 8 (Sonic
			 * Focus)
			 */
			if (getEffected().getKnownSkill(8) != null || getEffected().getKnownSkill(50) != null)
			{
				/*
				 * First we must check if he still deserves to receive the
				 * force...
				 */
				EffectRadiusSkill.getInstance().checkRadiusSkills(getEffected() , false);
				/*
				 * We give him the skill 2165 : Energy Stone to raise Force by
				 * one
				 */
				getEffected().doCast(net.sf.l2j.gameserver.datatables.SkillTable.getInstance().getInfo(2165, 1));
			}

		}

		double manaDam = calc();

		if (manaDam > getEffected().getStatus().getCurrentMp())
		{
			if (getSkill().isToggle())
			{
				SystemMessage sm = new SystemMessage(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP);
				getEffected().sendPacket(sm);
				return false;
			}
		}

		getEffected().reduceCurrentMp(manaDam);
		return true;
	}
}
