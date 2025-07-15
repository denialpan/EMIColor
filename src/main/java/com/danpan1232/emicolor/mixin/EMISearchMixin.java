package com.danpan1232.emicolor.mixin;

import dev.emi.emi.search.Query;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Function;

@Mixin(targets = "dev.emi.emi.search.EmiSearch$CompiledQuery")
public class EMISearchMixin {

    @Redirect(
            method = "<init>(Ljava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/emi/emi/search/EmiSearch$CompiledQuery;addQuery(Ljava/lang/String;ZLjava/util/List;Ljava/util/function/Function;Ljava/util/function/Function;)V"
            )
    )
    private void redirectAddQuery(String s, boolean negated, List<Query> queries,
                                  Function<String, Query> normal, Function<String, Query> regex) {
        Query q;
        if (s.startsWith("color:")) {
            String hex = s.substring("color:".length());
            q = new com.danpan1232.emicolor.ColorQuery(hex);
        } else if (s.length() > 1 && s.startsWith("/") && s.endsWith("/")) {
            q = regex.apply(s.substring(1, s.length() - 1));
        } else if (s.length() > 1 && s.startsWith("\"") && s.endsWith("\"")) {
            q = normal.apply(s.substring(1, s.length() - 1));
        } else {
            q = normal.apply(s);
        }
        q.negated = negated;
        queries.add(q);
    }
}

