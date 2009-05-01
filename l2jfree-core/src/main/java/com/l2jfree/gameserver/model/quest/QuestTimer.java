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
package com.l2jfree.gameserver.model.quest;

import java.util.concurrent.ScheduledFuture;

import com.l2jfree.gameserver.ThreadPoolManager;
import com.l2jfree.gameserver.model.actor.L2Npc;
import com.l2jfree.gameserver.model.actor.instance.L2PcInstance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class QuestTimer
{
    protected final static Log _log = LogFactory.getLog(QuestTimer.class.getName());

    // =========================================================
    // Schedule Task
    public class ScheduleTimerTask implements Runnable
    {
        public void run()
        {
            if (this == null || !getIsActive()) return;

            try
            {
                if (!getIsRepeating())
                    cancel();
                getQuest().notifyEvent(getName(), getNpc(), getPlayer());
            }
            catch (Exception e)
            {
                _log.error(e.getMessage(), e);
            }
        }
    }

    // =========================================================
    // Data Field
    private boolean _isActive = true;
    private String _name;
    private Quest _quest;
    private L2Npc _npc;
    private L2PcInstance _player;
    private boolean _isRepeating;
    private ScheduledFuture<?> _scheduler;

    // =========================================================
    // Constructor
    public QuestTimer(Quest quest, String name, long time, L2Npc npc, L2PcInstance player, boolean repeating)
    {
        _name = name;
        _quest = quest;
        _player = player;
        _npc = npc;
        _isRepeating = repeating;
        if (repeating)
            _scheduler = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ScheduleTimerTask(), time, time); // Prepare auto end task
        else
            _scheduler = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask(), time); // Prepare auto end task
    }

    public QuestTimer(Quest quest, String name, long time, L2Npc npc, L2PcInstance player)
    {
        this(quest, name, time, npc, player, false);
    }

    public QuestTimer(QuestState qs, String name, long time)
    {
        this(qs.getQuest(), name, time, null, qs.getPlayer(), false);
    }

    // =========================================================
    // Method - Public
    public void cancel()
    {
        _isActive = false;

        if (_scheduler != null) _scheduler.cancel(true);

        getQuest().removeQuestTimer(this);
    }
    
    /**
     * public method to compare if this timer matches with the key attributes passed.
     * @param quest : Quest instance to which the timer is attached
     * @param name : Name of the timer
     * @param npc : Npc instance attached to the desired timer (null if no npc attached)
     * @param player : Player instance attached to the desired timer (null if no player attached)
     */
    public boolean isMatch(Quest quest, String name, L2Npc npc, L2PcInstance player)
    {
        if (quest == null || name == null)
            return false;
        if (quest != getQuest() || !name.equalsIgnoreCase(getName()))
            return false;
        return (npc == getNpc() && player == getPlayer());
    }

    // =========================================================
    // Property - Public
    public final boolean getIsActive()
    {
        return _isActive;
    }

    public final boolean getIsRepeating()
    {
        return _isRepeating;
    }

    public final Quest getQuest()
    {
        return _quest;
    }
    
    public final String getName()
    {
        return _name;
    }
    
    public final L2Npc getNpc()
    {
    	return _npc;
    }
    
    public final L2PcInstance getPlayer()
    {
    	return _player;
    }
    
    @Override
    public final String toString()
    {
        return _name;
    }
}
