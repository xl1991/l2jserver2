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
package script.template.actor.npc.raidboss;

import com.google.inject.Inject;
import com.l2jserver.model.id.template.provider.ItemTemplateIDProvider;
import com.l2jserver.model.id.template.provider.NPCTemplateIDProvider;
import com.l2jserver.model.template.npc.RaidBossNPCTemplate;
import com.l2jserver.model.world.Actor.ActorSex;

/**
 * @author <a href="http://www.rogiel.com">Rogiel</a>
 */
public class TearsTemplate extends RaidBossNPCTemplate {
	public static final int ID = 25534;

	@Inject
	protected TearsTemplate(NPCTemplateIDProvider provider, ItemTemplateIDProvider itemProvider) {
		super(provider.createID(ID));
		this.name = "Tears";
		this.serverSideName = false;
		this.title = "Ice";
		this.serverSideTitle = false;
		this.collisionRadius = 20.00;
		this.collisionHeight = 27.50;
		this.level = 83;
		this.sex = ActorSex.FEMALE;
		this.attackRange = 40;
		this.maxHP = 1347510.706572180000000;
		this.maxMP = 1777.400000000000000;
		this.hpRegeneration = 152.563941686569000;
		this.mpRegeneration = 3.000000000000000;
		this.experience = 2010425;
		this.sp = 1208631;
		this.aggressive = false;
		this.rightHand = itemProvider.createID(9638);
		this.leftHand = itemProvider.createID(9638);
		this.enchantLevel = 0;
		this.targetable = true;
		this.showName = true;
		this.dropHerbGroup = 0;
		this.baseAttributes = true;
		
		attributes.intelligence = 21;
		attributes.strength = 40;
		attributes.concentration = 43;
		attributes.mentality = 80;
		attributes.dexterity = 30;
		attributes.witness = 20;
		attributes.physicalAttack = 5327.46513;
		attributes.magicalAttack = 3637.96805;
		attributes.physicalDefense = 636.95059;
		attributes.magicalDefense = 342.08100;
		attributes.attackSpeed = 253;
		attributes.castSpeed = 333;
		attributes.criticalChance = 4;
		attributes.walkSpeed = 60.00000;
		attributes.runSpeed = 180.00000;
	}
}