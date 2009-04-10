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
package com.l2jfree.gameserver.taskmanager;

import java.util.Set;

import javolution.util.FastSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.l2jfree.gameserver.ThreadPoolManager;
import com.l2jfree.gameserver.model.actor.status.CharStatus;

public final class RegenTaskManager implements Runnable
{
	private static final Log _log = LogFactory.getLog(RegenTaskManager.class);
	
	private static RegenTaskManager _instance;
	
	public static RegenTaskManager getInstance()
	{
		if (_instance == null)
			_instance = new RegenTaskManager();
		
		return _instance;
	}
	
	private final Set<CharStatus> _startList = new FastSet<CharStatus>();
	private final Set<CharStatus> _stopList = new FastSet<CharStatus>();
	
	private final Set<CharStatus> _regenTasks = new FastSet<CharStatus>();
	
	private RegenTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 100, 1000);
		
		_log.info("RegenTaskManager: Initialized.");
	}
	
	public synchronized boolean hasRegenTask(CharStatus status)
	{
		return _regenTasks.contains(status) || _startList.contains(status);
	}
	
	public synchronized void startRegenTask(CharStatus status)
	{
		_startList.add(status);
		
		_stopList.remove(status);
	}
	
	public synchronized void stopRegenTask(CharStatus status)
	{
		_stopList.add(status);
		
		_startList.remove(status);
	}
	
	public void run()
	{
		synchronized (this)
		{
			_regenTasks.addAll(_startList);
			_regenTasks.removeAll(_stopList);
			
			_startList.clear();
			_stopList.clear();
		}
		
		for (CharStatus status : _regenTasks)
			status.regenTask();
	}
}