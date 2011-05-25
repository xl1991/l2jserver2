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
public class RangerKarankawaTemplate extends RaidBossNPCTemplate {
	public static final int ID = 25557;

	@Inject
	protected RangerKarankawaTemplate(NPCTemplateIDProvider provider, ItemTemplateIDProvider itemProvider) {
		super(provider.createID(ID));
		this.name = "Ranger Karankawa";
		this.serverSideName = false;
		this.title = "Forsaken Prisoner";
		this.serverSideTitle = false;
		this.collisionRadius = 60.00;
		this.collisionHeight = 47.00;
		this.level = 79;
		this.sex = ActorSex.FEMALE;
		this.attackRange = 40;
		this.maxHP = 207500.502974975000000;
		this.maxMP = 1641.000000000000000;
		this.hpRegeneration = 88.699884116806000;
		this.mpRegeneration = 3.000000000000000;
		this.experience = 2571289;
		this.sp = 385228;
		this.aggressive = false;
		this.rightHand = null;
		this.leftHand = null;
		this.enchantLevel = 0;
		this.targetable = true;
		this.showName = true;
		this.dropHerbGroup = 0;
		this.baseAttributes = true;
		
		attributes.intelligence = 76;
		attributes.strength = 60;
		attributes.concentration = 57;
		attributes.mentality = 80;
		attributes.dexterity = 73;
		attributes.witness = 70;
		attributes.physicalAttack = 7315.33208;
		attributes.magicalAttack = 960.30752;
		attributes.physicalDefense = 1011.21024;
		attributes.magicalDefense = 493.30000;
		attributes.attackSpeed = 253;
		attributes.castSpeed = 333;
		attributes.criticalChance = 4;
		attributes.walkSpeed = 60.00000;
		attributes.runSpeed = 170.00000;
	}
}