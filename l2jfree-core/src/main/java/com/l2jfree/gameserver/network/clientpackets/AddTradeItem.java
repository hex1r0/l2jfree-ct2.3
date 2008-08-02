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
package com.l2jfree.gameserver.network.clientpackets;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.l2jfree.Config;
import com.l2jfree.gameserver.Shutdown;
import com.l2jfree.gameserver.model.L2World;
import com.l2jfree.gameserver.model.TradeList;
import com.l2jfree.gameserver.model.actor.instance.L2PcInstance;
import com.l2jfree.gameserver.network.SystemMessageId;
import com.l2jfree.gameserver.network.serverpackets.ActionFailed;
import com.l2jfree.gameserver.network.serverpackets.SystemMessage;
import com.l2jfree.gameserver.network.serverpackets.TradeOtherAdd;
import com.l2jfree.gameserver.network.serverpackets.TradeOwnAdd;

/**
 * This class ...
 * 
 * @version $Revision: 1.5.2.2.2.5 $ $Date: 2005/03/27 15:29:29 $
 */
public class AddTradeItem extends L2GameClientPacket
{
    private static final String _C__16_ADDTRADEITEM = "[C] 16 AddTradeItem";
    private final static Log _log = LogFactory.getLog(AddTradeItem.class.getName());

    @SuppressWarnings("unused")
    private int _tradeId;
    private int _objectId;
    private int _count;

    @Override
    protected void readImpl()
    {
        _tradeId = readD();
        _objectId = readD();
        _count = readD();
    }


    @Override
    protected void runImpl()
    {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) return;

        if (Config.SAFE_REBOOT && Config.SAFE_REBOOT_DISABLE_TRANSACTION && Shutdown.getCounterInstance() != null 
                && Shutdown.getCounterInstance().getCountdown() <= Config.SAFE_REBOOT_TIME)
        {
            player.sendMessage("Transactions are not allowed during restart/shutdown.");
            player.sendPacket(ActionFailed.STATIC_PACKET);
            player.cancelActiveTrade();
            return;
        }
        
        TradeList trade = player.getActiveTradeList();
        if (trade == null)
        {
            _log.warn("Character: " + player.getName() + " requested item:" + _objectId + " add without active tradelist:" + _tradeId);
            return;
        }

        if (trade.getPartner() == null || L2World.getInstance().findObject(trade.getPartner().getObjectId()) == null)
        {
            // Trade partner not found, cancel trade
            if (trade.getPartner() != null)
                _log.warn("Character:" + player.getName() + " requested invalid trade object: " + _objectId); 
            SystemMessage msg = new SystemMessage(SystemMessageId.TARGET_IS_NOT_FOUND_IN_THE_GAME);
            player.sendPacket(msg);
            player.cancelActiveTrade();
            msg = null;
            return;
        }

        if (Config.GM_DISABLE_TRANSACTION && player.getAccessLevel() >= Config.GM_TRANSACTION_MIN
            && player.getAccessLevel() <= Config.GM_TRANSACTION_MAX)
        {
            player.sendMessage("Unsufficient privileges.");
            player.cancelActiveTrade();
            return;
        }

        if (!player.validateItemManipulation(_objectId, "trade") && !player.isGM())
        {
            player.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
            return;
        }

        TradeList.TradeItem item = trade.addItem(_objectId, _count);
        if (item != null)
        {
            player.sendPacket(new TradeOwnAdd(item));
            trade.getPartner().sendPacket(new TradeOtherAdd(item));
        }
    }

    /* (non-Javadoc)
     * @see com.l2jfree.gameserver.clientpackets.ClientBasePacket#getType()
     */
    @Override
    public String getType()
    {
        return _C__16_ADDTRADEITEM;
    }
}
