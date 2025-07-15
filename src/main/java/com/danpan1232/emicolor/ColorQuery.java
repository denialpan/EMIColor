package com.danpan1232.emicolor;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.search.Query;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.network.chat.Component;

public class ColorQuery extends Query {
    private final int[] targetRGB;
    private static final Pattern HEX_PATTERN = Pattern.compile("#[0-9a-fA-F]{6}");

    public ColorQuery(String hex) {
        this.targetRGB = hexToRGB(hex);
    }

    @Override
    public boolean matches(EmiStack stack) {
        List<Component> tooltip = stack.getTooltipText();
        if (tooltip == null || tooltip.isEmpty()) return false;

        for (int i = 1; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            if (line == null) continue;

            String text = line.getString();
            Matcher matcher = HEX_PATTERN.matcher(text);
            while (matcher.find()) {
                String hex = matcher.group();
                int[] foundRGB = hexToRGB(hex);
                if (colorSimilarity(targetRGB, foundRGB) >= 0.80 && foundRGB[0] >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private int[] hexToRGB(String hex) {
        hex = hex.replace("#", "").trim();

        // Defensive check
        if (hex.length() != 6) {
            return new int[] { -1, -1, -1 };
        }

        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new int[]{r, g, b};
        } catch (NumberFormatException e) {
            return new int[] { -1, -1, -1 };
        }
    }


    private double colorSimilarity(int[] a, int[] b) {
        double distance = Math.sqrt(
                Math.pow(a[0] - b[0], 2) +
                        Math.pow(a[1] - b[1], 2) +
                        Math.pow(a[2] - b[2], 2)
        );
        return 1.0 - (distance / 441.6729559);
    }
}
