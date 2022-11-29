/*
 *  Copyright (C) <2022> <XiaoMoMi>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.customcrops.integrations.customplugin.oraxen;

import net.momirealms.customcrops.CustomCrops;
import net.momirealms.customcrops.api.crop.Crop;
import net.momirealms.customcrops.config.BasicItemConfig;
import net.momirealms.customcrops.config.CropConfig;
import net.momirealms.customcrops.config.MainConfig;
import net.momirealms.customcrops.integrations.customplugin.CustomInterface;
import net.momirealms.customcrops.managers.CropManager;
import net.momirealms.customcrops.managers.CropModeInterface;
import net.momirealms.customcrops.objects.GiganticCrop;
import net.momirealms.customcrops.objects.fertilizer.Fertilizer;
import net.momirealms.customcrops.objects.fertilizer.Gigantic;
import net.momirealms.customcrops.objects.fertilizer.SpeedGrow;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class OraxenWireCropImpl implements CropModeInterface {

    private final CropManager cropManager;
    private final CustomInterface customInterface;

    public OraxenWireCropImpl(CropManager cropManager) {
        this.cropManager = cropManager;
        this.customInterface = cropManager.getCustomInterface();
    }

    @Override
    public boolean growJudge(Location location) {
        String blockID = customInterface.getBlockID(location);
        if (blockID == null || !blockID.contains("_stage_")) return true;

        String[] cropNameList = StringUtils.split(blockID,"_");
        Crop crop = CropConfig.CROPS.get(cropNameList[0]);
        if (crop == null) return true;

        if ((MainConfig.needSkyLight && location.getBlock().getLightFromSky() < MainConfig.skyLightLevel) || cropManager.isWrongSeason(location, crop.getSeasons())) {
            Bukkit.getScheduler().runTask(CustomCrops.plugin, () -> customInterface.placeWire(location, BasicItemConfig.deadCrop));
            return true;
        }

        Location potLoc = location.clone().subtract(0,1,0);
        String potID = customInterface.getBlockID(potLoc);
        if (potID == null || (!potID.equals(BasicItemConfig.wetPot) && !potID.equals(BasicItemConfig.dryPot))) return true;

        if (MainConfig.enableCrow && cropManager.crowJudge(location)) return true;

        Fertilizer fertilizer = cropManager.getFertilizer(potLoc);
        boolean certainGrow = potID.equals(BasicItemConfig.wetPot);
        int nextStage = Integer.parseInt(cropNameList[2]) + 1;

        String temp = cropNameList[0] + "_" + cropNameList[1] + "_";
        if (customInterface.doesExist(temp + nextStage)) {
            if (fertilizer instanceof SpeedGrow speedGrow
                    && Math.random() < speedGrow.getChance()
                    && customInterface.doesExist(temp + (nextStage+1))
            )
                addStage(location, temp + (nextStage+1));
            else if (certainGrow || Math.random() < MainConfig.dryGrowChance)
                addStage(location, temp + nextStage);
            return false;
        }
        else {
            GiganticCrop giganticCrop = crop.getGiganticCrop();
            if (giganticCrop != null) {
                double chance = giganticCrop.getChance();
                if (fertilizer instanceof Gigantic gigantic) chance += gigantic.getChance();
                if (Math.random() < chance) {
                    Bukkit.getScheduler().runTask(CustomCrops.plugin, () -> {
                        customInterface.removeBlock(location);
                        if (giganticCrop.isBlock()) customInterface.placeWire(location, giganticCrop.getBlockID());
                        else customInterface.placeFurniture(location, giganticCrop.getBlockID());
                    });
                }
            }
            return true;
        }
    }

    private void addStage(Location seedLoc, String stage) {
        Bukkit.getScheduler().runTask(CustomCrops.plugin, () -> customInterface.placeWire(seedLoc, stage));
    }
}
