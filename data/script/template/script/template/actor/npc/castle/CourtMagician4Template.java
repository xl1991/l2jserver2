/*
 * This file is part of l2jserver <l2jserver.com>.
 *
 * l2jserver is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * l2jserver is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with l2jserver.  If not, see <http://www.gnu.org/licenses/>.
 */
package script.template.actor.npc.castle;

import com.google.inject.Inject;
import com.l2jserver.model.id.template.provider.ItemTemplateIDProvider;
import com.l2jserver.model.id.template.provider.NPCTemplateIDProvider;
import com.l2jserver.model.template.npc.CastleMagicianNPCTemplate;
import com.l2jserver.model.world.Actor.ActorSex;

/**
 * @author <a href="http://www.rogiel.com">Rogiel</a>
 */
public class CourtMagician4Template extends CastleMagicianNPCTemplate {
	public static final int ID = 35650;

	@Inject
	protected CourtMagician4Template(NPCTemplateIDProvider provider, ItemTemplateIDProvider itemProvider) {
		super(provider.createID(ID));
		this.name = "Court Magician";
		this.serverSideName = false;
		this.title = "Giran";
		this.serverSideTitle = false;
		this.collisionRadius = 6.50;
		this.collisionHeight = 21.96;
		this.level = 70;
		this.sex = ActorSex.FEMALE;
		this.attackRange = 40;
		this.maxHP = 2444.468190000000000;
		this.maxMP = 1345.800000000000000;
		this.hpRegeneration = 7.500000000000000;
		this.mpRegeneration = 2.700000000000000;
		this.experience = 0;
		this.sp = 0;
		this.aggressive = false;
		this.rightHand = itemProvider.createID(8687);
		this.leftHand = null;
		this.enchantLevel = 0;
		this.targetable = true;
		this.showName = true;
		this.dropHerbGroup = 0;
		this.baseAttributes = true;
		
		attributes.intelligence = 41;
		attributes.strength = 22;
		attributes.concentration = 27;
		attributes.mentality = 20;
		attributes.dexterity = 21;
		attributes.witness = 20;
		attributes.physicalAttack = 688.86373;
		attributes.magicalAttack = 470.40463;
		attributes.physicalDefense = 295.91597;
		attributes.magicalDefense = 216.53847;
		attributes.attackSpeed = 253;
		attributes.castSpeed = 333;
		attributes.criticalChance = 4;
		attributes.walkSpeed = 33.00000;
		attributes.runSpeed = 120.00000;
	}
}