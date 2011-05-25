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
public class ShadithsSentinelTemplate extends MonsterNPCTemplate {
	public static final int ID = 25311;

	@Inject
	protected ShadithsSentinelTemplate(NPCTemplateIDProvider provider, ItemTemplateIDProvider itemProvider) {
		super(provider.createID(ID));
		this.name = "Shadith's Sentinel";
		this.serverSideName = false;
		this.title = "Raid Fighter";
		this.serverSideTitle = false;
		this.collisionRadius = 16.00;
		this.collisionHeight = 38.00;
		this.level = 79;
		this.sex = ActorSex.MALE;
		this.attackRange = 40;
		this.maxHP = 23781.195532672700000;
		this.maxMP = 1641.000000000000000;
		this.hpRegeneration = 91.493086301435800;
		this.mpRegeneration = 3.000000000000000;
		this.experience = 0;
		this.sp = 0;
		this.aggressive = false;
		this.rightHand = itemProvider.createID(2504);
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
		attributes.physicalAttack = 2848.82203;
		attributes.magicalAttack = 1105.91357;
		attributes.physicalDefense = 505.60512;
		attributes.magicalDefense = 493.30000;
		attributes.attackSpeed = 253;
		attributes.castSpeed = 333;
		attributes.criticalChance = 8;
		attributes.walkSpeed = 44.00000;
		attributes.runSpeed = 190.00000;
	}
}