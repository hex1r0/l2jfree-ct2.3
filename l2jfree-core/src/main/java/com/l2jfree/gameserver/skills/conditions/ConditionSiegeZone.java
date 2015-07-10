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
package com.l2jfree.gameserver.skills.conditions;

import com.l2jfree.gameserver.gameobjects.L2Creature;
import com.l2jfree.gameserver.gameobjects.instance.L2PcInstance;
import com.l2jfree.gameserver.instancemanager.CastleManager;
import com.l2jfree.gameserver.instancemanager.ClanHallManager;
import com.l2jfree.gameserver.instancemanager.FortManager;
import com.l2jfree.gameserver.model.entity.Castle;
import com.l2jfree.gameserver.model.entity.ClanHall;
import com.l2jfree.gameserver.model.entity.Fort;
import com.l2jfree.gameserver.skills.Env;

/**
 * @author Gigiikun
 */

final class ConditionSiegeZone extends Condition
{
	//	conditional values
	public final static int COND_NOT_ZONE = 0x0001;
	public final static int COND_CAST_ATTACK = 0x0002;
	public final static int COND_CAST_DEFEND = 0x0004;
	public final static int COND_CAST_NEUTRAL = 0x0008;
	public final static int COND_FORT_ATTACK = 0x0010;
	public final static int COND_FORT_DEFEND = 0x0020;
	public final static int COND_FORT_NEUTRAL = 0x0040;
	
	private final int _value;
	private final boolean _self;
	
	public ConditionSiegeZone(int value, boolean self)
	{
		_value = value;
		_self = self;
	}
	
	@Override
	public boolean testImpl(Env env)
	{
		L2Creature target = _self ? env.player : env.target;
		Castle castle = CastleManager.getInstance().getCastle(target);
		Fort fort = FortManager.getInstance().getFort(target);
		ClanHall ch = ClanHallManager.getInstance().getClanHall(target);
		
		if (castle == null && fort == null && (ch == null || ch.getSiege() == null))
		{
			if ((_value & COND_NOT_ZONE) != 0)
				return true;
			else
				return false;
		}
		if (castle != null)
			return checkIfOk(target, castle, _value);
		else if (fort != null)
			return checkIfOk(target, fort, _value);
		else
			return checkIfOk(target, ch, _value);
	}
	
	public static boolean checkIfOk(L2Creature activeChar, Castle castle, int value)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
			return false;
		
		L2PcInstance player = (L2PcInstance)activeChar;
		
		if (castle == null || castle.getCastleId() <= 0)
		{
			if ((value & COND_NOT_ZONE) != 0)
				return true;
		}
		else if (!castle.getSiege().getIsInProgress())
		{
			if ((value & COND_NOT_ZONE) != 0)
				return true;
		}
		else if (castle.getSiege().getAttackerClan(player.getClan()) != null && (value & COND_CAST_ATTACK) != 0)
			return true;
		else if (castle.getSiege().getDefenderClan(player.getClan()) != null && (value & COND_CAST_DEFEND) != 0)
			return true;
		else if (castle.getSiege().getAttackerClan(player.getClan()) == null
				&& castle.getSiege().getDefenderClan(player.getClan()) == null && (value & COND_CAST_NEUTRAL) != 0)
			return true;
		
		return false;
	}
	
	public static boolean checkIfOk(L2Creature activeChar, Fort fort, int value)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
			return false;
		
		L2PcInstance player = (L2PcInstance)activeChar;
		
		if (fort == null || fort.getFortId() <= 0)
		{
			if ((value & COND_NOT_ZONE) != 0)
				return true;
		}
		else if (!fort.getSiege().getIsInProgress())
		{
			if ((value & COND_NOT_ZONE) != 0)
				return true;
		}
		else if (fort.getSiege().getAttackerClan(player.getClan()) != null && (value & COND_FORT_ATTACK) != 0)
			return true;
		else if (fort.getOwnerClan() == player.getClan() && (value & COND_FORT_DEFEND) != 0)
			return true;
		else if (fort.getSiege().getAttackerClan(player.getClan()) == null && fort.getOwnerClan() != player.getClan()
				&& (value & COND_FORT_NEUTRAL) != 0)
			return true;
		
		return false;
	}
	
	public static boolean checkIfOk(L2Creature activeChar, ClanHall ch, int value)
	{
		if (activeChar == null || !(activeChar instanceof L2PcInstance))
			return false;
		
		L2PcInstance player = activeChar.getActingPlayer();
		
		if (ch == null || ch.getSiege() == null)
		{
			if ((value & COND_NOT_ZONE) != 0)
				return true;
		}
		else if (!ch.getSiege().getIsInProgress())
		{
			if ((value & COND_NOT_ZONE) != 0)
				return true;
		}
		else if (ch.getSiege().getAttackerClan(player.getClan()) != null && (value & COND_CAST_ATTACK) != 0)
			return true;
		else if (ch.getSiege().getDefenderClan(player.getClan()) != null && (value & COND_CAST_DEFEND) != 0)
			return true;
		else if (ch.getSiege().getAttackerClan(player.getClan()) == null
				&& ch.getSiege().getDefenderClan(player.getClan()) == null && (value & COND_CAST_NEUTRAL) != 0)
			return true;
		
		return false;
	}
}
