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
package com.l2jfree.util.logging;

import java.util.logging.ConsoleHandler;

/**
 * @author NB4L1
 */
public final class ConsoleLog
{
	private ConsoleLog()
	{
	}
	
	public static final class Handler extends ConsoleHandler
	{
		public Handler()
		{
			super();
			
			setOutputStream(System.out);
		}
	}
	
	public static final class Formatter extends L2RuntimeLogFormatter
	{
	}
}
