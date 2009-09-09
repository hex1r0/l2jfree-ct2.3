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
package transformations;

import com.l2jfree.gameserver.instancemanager.TransformationManager;
import com.l2jfree.gameserver.model.L2Transformation;
import com.l2jfree.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Ahmed
 */
public class DollBlader extends L2Transformation
{
	public DollBlader()
	{
		// id, colRadius, colHeight
		super(7, 6.0, 12.0);
	}
	
	@Override
	public void transformedSkills(L2PcInstance player)
	{
		int level = 1;
		if (player.getLevel() >= 76)
			level = 3;
		else if (player.getLevel() >= 73)
			level = 2;
		
		addSkill(player, 752, 1);
		addSkill(player, 753, level);
		addSkill(player, 754, level);
	}
	
	@Override
	public void removeSkills(L2PcInstance player)
	{
		removeSkill(player, 752);
		removeSkill(player, 753);
		removeSkill(player, 754);
	}
	
	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new DollBlader());
	}
}
