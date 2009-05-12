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
package com.l2jfree.gameserver.skills.effects;

import com.l2jfree.gameserver.model.L2Effect;
import com.l2jfree.gameserver.skills.Env;
import com.l2jfree.gameserver.templates.skills.L2EffectType;

/**
 * <B>Once applied, this effect is active until:</B>
 * <LI>Time limit (<I>count * time</I>) expires</LI>
 * <LI>Manually removed</LI><BR>
 * <U>It will not be removed under any other circumstances!</U>
 * @author Savormix
 * @since 2009-04-25
 */
public class EffectEnvironment extends L2Effect
{
	/**
	 * Default constructor.
	 * @param env
	 * @param template
	 */
	public EffectEnvironment(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	/* (non-Javadoc)
	 * @see com.l2jfree.gameserver.model.L2Effect#getEffectType()
	 */
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.ENVIRONMENT;
	}

	/* (non-Javadoc)
	 * @see com.l2jfree.gameserver.model.L2Effect#onActionTime()
	 */
	@Override
	protected boolean onActionTime()
	{
		return true;
	}
	
	@Override
	public final boolean canBeStoredInDb()
	{
		return false;
	}
}
