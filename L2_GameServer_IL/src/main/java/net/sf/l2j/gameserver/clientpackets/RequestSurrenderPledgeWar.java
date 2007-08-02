package net.sf.l2j.gameserver.clientpackets;

import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.serverpackets.SystemMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RequestSurrenderPledgeWar extends L2GameClientPacket
{
    private static final String _C__51_REQUESTSURRENDERPLEDGEWAR = "[C] 51 RequestSurrenderPledgeWar";
    private final static Log _log = LogFactory.getLog(RequestSurrenderPledgeWar.class.getName());

    String _pledgeName;
    L2Clan _clan;
    L2PcInstance player;
    
    protected void readImpl()
    {
        _pledgeName  = readS();
    }

    protected void runImpl()
    {
        player = getClient().getActiveChar();
	if (player == null)
	    return;
        _clan = player.getClan();
	if (_clan == null)
	    return;
        L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);

        if(clan == null)
        {
            player.sendMessage("No such clan.");
            player.sendPacket(new ActionFailed());
            return;                        
        }

        _log.info("RequestSurrenderPledgeWar by "+getClient().getActiveChar().getClan().getName()+" with "+_pledgeName);
        
        if(!_clan.isAtWarWith(clan.getClanId()))
        {
            player.sendMessage("You aren't at war with this clan.");
            player.sendPacket(new ActionFailed());
            return;            
        }
        
        
        SystemMessage msg = new SystemMessage(SystemMessageId.YOU_HAVE_SURRENDERED_TO_THE_S1_CLAN);
        msg.addString(_pledgeName);
        player.sendPacket(msg);
        msg = null;
        player.deathPenalty(false);
        ClanTable.getInstance().deleteclanswars(_clan.getClanId(), clan.getClanId());
        /*L2PcInstance leader = L2World.getInstance().getPlayer(clan.getLeaderName());
        if(leader != null && leader.isOnline() == 0)
        {
            player.sendMessage("Clan leader isn't online.");
            player.sendPacket(new ActionFailed());
            return;                        
        }
        
        if (leader.isTransactionInProgress())
        {
            SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_BUSY_TRY_LATER);
            sm.addString(leader.getName());
            player.sendPacket(sm);
            return;
        } 
        
        leader.setTransactionRequester(player);
        player.setTransactionRequester(leader);
        leader.sendPacket(new SurrenderPledgeWar(_clan.getName(),player.getName()));*/
    }
    
    public String getType()
    {
        return _C__51_REQUESTSURRENDERPLEDGEWAR;
    }
}