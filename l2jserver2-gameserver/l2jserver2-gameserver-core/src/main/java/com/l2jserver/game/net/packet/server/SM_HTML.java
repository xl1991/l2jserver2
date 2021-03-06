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
package com.l2jserver.game.net.packet.server;

import org.htmlparser.tags.Html;
import org.jboss.netty.buffer.ChannelBuffer;

import com.l2jserver.model.world.NPC;
import com.l2jserver.service.network.model.Lineage2Client;
import com.l2jserver.service.network.model.packet.AbstractServerPacket;
import com.l2jserver.util.BufferUtils;
import com.l2jserver.util.html.markup.HtmlTemplate;

/**
 * This packet sends an HTML message to be displayed in the client.
 * 
 * @author <a href="http://www.rogiel.com">Rogiel</a>
 */
public class SM_HTML extends AbstractServerPacket {
	/**
	 * The packet OPCODE
	 */
	public static final int OPCODE = 0x0f;

	/**
	 * The saying NPC
	 */
	private final NPC npc;
	/**
	 * The HTML contents
	 */
	private final String html;

	/**
	 * @param npc
	 *            the npc instance
	 * @param html
	 *            the html
	 */
	public SM_HTML(NPC npc, String html) {
		super(OPCODE);
		this.npc = npc;
		this.html = html;
	}

	/**
	 * @param npc
	 *            the npc instance
	 * @param html
	 *            the html
	 */
	public SM_HTML(NPC npc, Html html) {
		super(OPCODE);
		this.npc = npc;
		this.html = html.toHtml();
	}

	/**
	 * @param npc
	 *            the npc instance
	 * @param template
	 *            the html template
	 */
	public SM_HTML(NPC npc, HtmlTemplate template) {
		super(OPCODE);
		this.npc = npc;
		this.html = template.toHtmlString();
	}

	/**
	 * @param html
	 *            the html
	 */
	public SM_HTML(String html) {
		this(null, html);
	}

	/**
	 * @param html
	 *            the html
	 */
	public SM_HTML(Html html) {
		this(null, html);
	}

	/**
	 * @param template
	 *            the html template
	 */
	public SM_HTML(HtmlTemplate template) {
		this(null, template);
	}

	@Override
	public void write(Lineage2Client conn, ChannelBuffer buffer) {
		buffer.writeInt((npc != null ? npc.getID().getID() : 0x01));
		BufferUtils.writeString(buffer, html);
		buffer.writeInt(0x00); // item id
	}
}
