/*
 * This file is part of l2jserver2 <l2jserver2.com>.
 *
 * l2jserver2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * l2jserver2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with l2jserver2.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.service.game.admin;

import com.l2jserver.model.world.L2Character;
import com.l2jserver.service.Service;

/**
 * This service handles GM in the server
 * 
 * @author <a href="http://www.rogiel.com">Rogiel</a>
 */
public interface GMService extends Service {
	/**
	 * Executes an GM command
	 * 
	 * @param character
	 *            the GM character
	 * @param command
	 *            the command
	 * @param args
	 *            the command arguments
	 */
	void command(L2Character character, String command, String... args);

	/**
	 * The base interface for GM commands
	 * 
	 * @author <a href="http://www.rogiel.com">Rogiel</a>
	 */
	public interface GMCommand {
		/**
		 * @param character
		 *            the GM character
		 * @param args
		 *            the command arguments
		 */
		void gm(L2Character character, String... args);
	}
}
