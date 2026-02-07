package com.gdg.unimatebackend.schedulepoll.service;

import com.gdg.unimatebackend.schedulepoll.entity.SchedulePollVote;
import com.gdg.unimatebackend.schedulepoll.entity.SchedulePollVoteSlot;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SchedulePollAutoFixService {

    public List<Integer> computeIntersectionSlots(List<SchedulePollVote> votes, long totalCount) {
        if (votes == null || votes.isEmpty()) return List.of();
        if (totalCount <= 0) return List.of();

        Map<Integer, Long> freq = new HashMap<>();

        for (SchedulePollVote vote : votes) {
            Set<Integer> unique = new HashSet<>();
            for (SchedulePollVoteSlot s : vote.getSlots()) unique.add(s.getSlotId());

            for (Integer slotId : unique) {
                freq.put(slotId, freq.getOrDefault(slotId, 0L) + 1L);
            }
        }

        List<Integer> intersection = new ArrayList<>();
        for (Map.Entry<Integer, Long> e : freq.entrySet()) {
            if (e.getValue() == totalCount) intersection.add(e.getKey());
        }
        intersection.sort(Comparator.naturalOrder());
        return intersection;
    }

    public Integer pickEarliestSlot(List<Integer> intersectionSlots) {
        if (intersectionSlots == null || intersectionSlots.isEmpty()) return null;
        return intersectionSlots.get(0);
    }
}