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

import javolution.util.FastList;

import com.l2jfree.Config;
import com.l2jfree.gameserver.instancemanager.CastleManager;
import com.l2jfree.gameserver.instancemanager.CastleManorManager;
import com.l2jfree.gameserver.instancemanager.CastleManorManager.SeedProduction;

/**
 * Format: (ch) dd [ddd]
 * @author -Wooden-
 *
 * d - manor id
 * d - size
 * [
 * d - seed id
 * d - sales
 * d - price
 * ]
 * @author l3x
 * 
 */
public class RequestSetSeed extends L2GameClientPacket
{
	private static final String _C__D0_0A_REQUESTSETSEED = "[C] D0:0A RequestSetSeed";

	private int _size;

	private int _manorId;

	private int[] _items; // _size*3
	
	/**
	 */
	@Override
	protected void readImpl()
	{
		_manorId = readD();
		_size = readD();
		if (_size * 12 > getByteBuffer().remaining() || _size > 500)
		{
			_size = 0;
			return;
		}

		_items = new int[_size * 3];
		for (int i = 0; i < _size; i++)
		{
			int itemId = readD();
			_items[(i * 3)] = itemId;
			
			int sales =0;
			if(Config.PACKET_FINAL)
				sales = toInt(readQ());
			else
				sales = readD();
			
			_items[i * 3 + 1] = sales;
			int price = 0;
			if(Config.PACKET_FINAL)
				price = toInt(readQ());
			else
				price = readD();
			_items[i * 3 + 2] = price;
		}
	}

	@Override
	protected void runImpl()
	{
		if (_size < 1)
			return;

		FastList<SeedProduction> seeds = new FastList<SeedProduction>();
		for (int i = 0; i < _size; i++)
		{
			int id = _items[(i * 3)];
			int sales = _items[i * 3 + 1];
			int price = _items[i * 3 + 2];
			if (id > 0)
			{
				SeedProduction s = CastleManorManager.getInstance()
						.getNewSeedProduction(id, sales, price, sales);
				seeds.add(s);
			}
		}

		CastleManager.getInstance().getCastleById(_manorId).setSeedProduction(seeds, CastleManorManager.PERIOD_NEXT);
		if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
			CastleManager.getInstance().getCastleById(_manorId).saveSeedData(CastleManorManager.PERIOD_NEXT);
	}

	@Override
	public String getType()
	{
		return _C__D0_0A_REQUESTSETSEED;
	}
}