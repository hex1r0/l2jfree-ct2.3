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
package com.l2jfree.gameserver.network.packets.client;

import com.l2jfree.gameserver.gameobjects.L2Player;
import com.l2jfree.gameserver.network.SystemMessageId;
import com.l2jfree.gameserver.network.packets.L2ClientPacket;

/**
 * This class ...
 * 
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestWithDrawalParty extends L2ClientPacket
{
	private static final String _C__2B_REQUESTWITHDRAWALPARTY = "[C] 2B RequestWithDrawalParty";
	
	@Override
	public String getType()
	{
		return _C__2B_REQUESTWITHDRAWALPARTY;
	}
	
	@Override
	protected void readImpl()
	{
		// trigger
	}
	
	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getActiveChar();
		if (player == null)
			return;
		
		if (player.isInParty())
			if (player.getParty().isInDimensionalRift()
					&& !player.getParty().getDimensionalRift().getRevivedAtWaitingRoom().contains(player))
				sendPacket(SystemMessageId.COULD_NOT_LEAVE_PARTY);
			else
				player.getParty().removePartyMember(player, false);
		
		sendAF();
	}
}
