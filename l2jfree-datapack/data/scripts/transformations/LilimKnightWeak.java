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
 * @author durgus
 */
public class LilimKnightWeak extends L2Transformation
{
	public LilimKnightWeak()
	{
		// id, colRadius, colHeight
		super(209, 8.0, 24.4);
	}
	
	@Override
	public void transformedSkills(L2PcInstance player)
	{
		addSkill(player, 568, 2); // Attack Buster
		//addSkill(player, 569, 2); // Attack Storm - Too few charges reachable to use this skill
		addSkill(player, 570, 2); // Attack Rage
		addSkill(player, 571, 2); // Poison Dust
		
		player.clearCharges();
	}
	
	@Override
	public void removeSkills(L2PcInstance player)
	{
		removeSkill(player, 568); // Attack Buster
		//removeSkill(player, 569); // Attack Storm - Too few charges reachable to use this skill
		removeSkill(player, 570); // Attack Rage
		removeSkill(player, 571); // Poison Dust
		
		player.clearCharges();
	}
	
	public static void main(String[] args)
	{
		TransformationManager.getInstance().registerTransformation(new LilimKnightWeak());
	}
}
