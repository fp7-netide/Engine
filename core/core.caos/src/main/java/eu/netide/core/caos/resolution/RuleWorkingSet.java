package eu.netide.core.caos.resolution;

import eu.netide.lib.netip.OpenFlowMessage;
import org.javatuples.Pair;
import org.projectfloodlight.openflow.protocol.OFFlowMod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by timvi on 07.09.2015.
 */
public class RuleWorkingSet {
    private List<OpenFlowMessage> messages = new ArrayList<>();

    public RuleWorkingSet() {

    }

    public RuleWorkingSet(Stream<OpenFlowMessage> messages) {
        messages.forEach(this.messages::add);
    }

    public boolean addDistinct(OpenFlowMessage flowMod) {
        if (!this.messages.contains(flowMod)) {
            this.messages.add(flowMod);
            return true;
        }
        return false;
    }

    public boolean addDistinctBasedOnMatch(OpenFlowMessage flowMod) {
        if (!this.messages.stream().anyMatch(m -> {
            List<OFMatchConflict> conflicts = ResolutionUtils.getMatchConflicts(m, flowMod, fM(m).getMatch(), fM(flowMod).getMatch()).collect(Collectors.toList());
            return conflicts.stream().allMatch(c -> c.getType() == OFMatchConflict.Type.Same) & conflicts.size() == ResolutionUtils.MATCH_FIELDS_TO_CHECK.length;
        })) {
            this.messages.add(flowMod);
            return true;
        }
        return false;
    }

    private OFFlowMod fM(OpenFlowMessage message) {
        return (OFFlowMod) message.getOfMessage();
    }

    public Stream<Pair<OpenFlowMessage, OpenFlowMessage>> getPairs() {
        return this.messages.stream().flatMap(m1 -> this.messages.stream().skip(this.messages.indexOf(m1) + 1).map(m2 -> new Pair<OpenFlowMessage, OpenFlowMessage>(m1, m2)));
    }

    public OpenFlowMessage[] toMessageArray() {
        return this.messages.stream().toArray(OpenFlowMessage[]::new);
    }

    public Stream<OpenFlowMessage> getMessages() {
        return this.messages.stream();
    }
}
