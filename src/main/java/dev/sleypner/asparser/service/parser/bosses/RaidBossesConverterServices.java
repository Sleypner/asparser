package dev.sleypner.asparser.service.parser.bosses;

import dev.sleypner.asparser.domain.model.Event;
import dev.sleypner.asparser.domain.model.RaidBoss;
import dev.sleypner.asparser.service.parser.util.RaidBossUtil;
import dev.sleypner.asparser.util.Util;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class RaidBossesConverterServices {

    public Set<RaidBoss> convert(Set<Event> events) {
        Set<RaidBoss> bosses = new HashSet<RaidBoss>();

        for (Event event : events) {
            String bossType = event.getType();
            if (bossType.equalsIgnoreCase("epic bosses") || bossType.equalsIgnoreCase("key bosses")) {
                String nameBoss = Util.getMatchedString("Boss ([\\s\\S]*?) was killed", event.getTitle());
                String killer = Util.getMatchedString("dealt by ([\\s\\S]*?) from clan", event.getDescription());
                String killerClan = "";
                if (killer != null) {
                    killerClan = Util.getMatchedString("clan (\\w+)\\.$", event.getDescription());
                } else {
                    killer = Util.getMatchedString("dealt by (\\w+)\\.$", event.getDescription());
                }
                String countAttackersString = Util.getMatchedString("attackers: ([\\s\\S]*?). ", event.getDescription());
                int countAttackers = 0;
                if (countAttackersString != null) {
                    countAttackers = Integer.parseInt(countAttackersString);
                }
                RaidBoss rb = RaidBoss.builder()
                        .name(nameBoss)
                        .type(bossType)
                        .server(event.getServer())
                        .lastKiller(killer)
                        .lastKillersClan(killerClan)
                        .attackersCount(countAttackers)
                        .countKilling(1)
                        .build()
                        .setDate(event.getDate());


                if (RaidBossUtil.exists(bosses, rb)) {
                    Optional<RaidBoss> optBoss = RaidBossUtil.findExists(bosses, rb);
                    if (optBoss.isPresent()) {
                        RaidBoss presBoss = optBoss.get();
                        if (presBoss.getDate().isBefore(rb.getDate())) {
                            rb.setCountKilling(presBoss.getCountKilling() + rb.getCountKilling());
                            bosses.remove(presBoss);
                            bosses.add(rb);
                        } else {
                            presBoss.setCountKilling(presBoss.getCountKilling() + rb.getCountKilling());
                        }
                    }
                } else {
                    bosses.add(rb);
                }
            }
        }
        return bosses;
    }
}
