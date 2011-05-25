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
package script.template.actor.npc.defender;

import com.google.inject.Inject;
import com.l2jserver.model.id.template.provider.ItemTemplateIDProvider;
import com.l2jserver.model.id.template.provider.NPCTemplateIDProvider;
import com.l2jserver.model.template.npc.DefenderNPCTemplate;
import com.l2jserver.model.world.Actor.ActorSex;

/**
 * @author <a href="http://www.rogiel.com">Rogiel</a>
 */
public class Guard89Template extends DefenderNPCTemplate {
	public static final int ID = 35291;

	@Inject
	protected Guard89Template(NPCTemplateIDProvider provider, ItemTemplateIDProvider itemProvider) {
		super(provider.createID(ID));
		this.name = "Guard";
		this.serverSideName = false;
		this.title = "Innadril";
		this.serverSideTitle = false;
		this.collisionRadius = 7.00;
		this.collisionHeight = 25.00;
		this.level = 75;
		this.sex = ActorSex.FEMALE;
		this.attackRange = 1100;
		this.maxHP = 2676.658540000000000;
		this.maxMP = 1507.800000000000000;
		this.hpRegeneration = 8.500000000000000;
		this.mpRegeneration = 3.000000000000000;
		this.experience = 563;
		this.sp = 1;
		this.aggressive = false;
		this.rightHand = itemProvider.createID(14);
		this.leftHand = null;
		this.enchantLevel = 0;
		this.targetable = true;
		this.showName = true;
		this.dropHerbGroup = 0;
		this.baseAttributes = true;
		
		attributes.intelligence = 21;
		attributes.strength = 40;
		attributes.concentration = 43;
		attributes.mentality = 20;
		attributes.dexterity = 30;
		attributes.witness = 20;
		attributes.physicalAttack = 794.70901;
		attributes.magicalAttack = 542.68324;
		attributes.physicalDefense = 319.24623;
		attributes.magicalDefense = 233.61053;
		attributes.attackSpeed = 253;
		attributes.castSpeed = 333;
		attributes.criticalChance = 8;
		attributes.walkSpeed = 50.00000;
		attributes.runSpeed = 120.00000;
	}
}