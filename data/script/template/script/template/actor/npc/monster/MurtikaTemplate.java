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
package script.template.actor.npc.monster;

import com.google.inject.Inject;
import com.l2jserver.model.id.template.provider.ItemTemplateIDProvider;
import com.l2jserver.model.id.template.provider.NPCTemplateIDProvider;
import com.l2jserver.model.template.npc.MonsterNPCTemplate;
import com.l2jserver.model.world.Actor.ActorSex;

/**
 * @author <a href="http://www.rogiel.com">Rogiel</a>
 */
public class MurtikaTemplate extends MonsterNPCTemplate {
	public static final int ID = 27068;

	@Inject
	protected MurtikaTemplate(NPCTemplateIDProvider provider, ItemTemplateIDProvider itemProvider) {
		super(provider.createID(ID));
		this.name = "Murtika";
		this.serverSideName = false;
		this.title = "Kaboo Chief";
		this.serverSideTitle = false;
		this.collisionRadius = 14.00;
		this.collisionHeight = 25.00;
		this.level = 8;
		this.sex = ActorSex.MALE;
		this.attackRange = 1100;
		this.maxHP = 124.928280000000000;
		this.maxMP = 92.640000000000000;
		this.hpRegeneration = 2.000000000000000;
		this.mpRegeneration = 0.900000000000000;
		this.experience = 0;
		this.sp = 0;
		this.aggressive = false;
		this.rightHand = itemProvider.createID(275);
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
		attributes.physicalAttack = 16.20165;
		attributes.magicalAttack = 11.06363;
		attributes.physicalDefense = 57.56203;
		attributes.magicalDefense = 42.12140;
		attributes.attackSpeed = 253;
		attributes.castSpeed = 333;
		attributes.criticalChance = 8;
		attributes.walkSpeed = 45.00000;
		attributes.runSpeed = 110.00000;
	}
}