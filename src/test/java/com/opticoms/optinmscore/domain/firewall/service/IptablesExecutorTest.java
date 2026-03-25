package com.opticoms.optinmscore.domain.firewall.service;

import com.opticoms.optinmscore.domain.firewall.model.FirewallRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IptablesExecutorTest {

    private IptablesExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new IptablesExecutor();
    }

    @Test
    void buildCommandArgs_fullRule_appendsAllFlags() {
        FirewallRule rule = buildRule(
                FirewallRule.Chain.INPUT, FirewallRule.Protocol.TCP,
                "10.0.0.0/8", "192.168.1.0/24",
                1234, 8080,
                FirewallRule.Action.ACCEPT, "eth0");

        List<String> args = executor.buildCommandArgs(rule, IptablesExecutor.Operation.APPEND);

        assertThat(args).containsExactly(
                "-A", "INPUT",
                "-p", "tcp",
                "-s", "10.0.0.0/8",
                "-d", "192.168.1.0/24",
                "--sport", "1234",
                "--dport", "8080",
                "-i", "eth0",
                "-j", "ACCEPT"
        );
    }

    @Test
    void buildCommandArgs_deleteOperation_usesMinusD() {
        FirewallRule rule = buildRule(
                FirewallRule.Chain.OUTPUT, FirewallRule.Protocol.UDP,
                null, null, null, 53,
                FirewallRule.Action.DROP, "ogstun");

        List<String> args = executor.buildCommandArgs(rule, IptablesExecutor.Operation.DELETE);

        assertThat(args.get(0)).isEqualTo("-D");
        assertThat(args.get(1)).isEqualTo("OUTPUT");
        assertThat(args).contains("-o", "ogstun");
    }

    @Test
    void buildCommandArgs_protocolAll_omitsProtocolFlag() {
        FirewallRule rule = buildRule(
                FirewallRule.Chain.FORWARD, FirewallRule.Protocol.ALL,
                null, null, null, null,
                FirewallRule.Action.REJECT, null);

        List<String> args = executor.buildCommandArgs(rule, IptablesExecutor.Operation.APPEND);

        assertThat(args).doesNotContain("-p");
        assertThat(args).containsExactly("-A", "FORWARD", "-j", "REJECT");
    }

    @Test
    void buildCommandArgs_noSourceNoDestNoPort_minimalRule() {
        FirewallRule rule = buildRule(
                FirewallRule.Chain.INPUT, FirewallRule.Protocol.ICMP,
                null, null, null, null,
                FirewallRule.Action.DROP, null);

        List<String> args = executor.buildCommandArgs(rule, IptablesExecutor.Operation.APPEND);

        assertThat(args).containsExactly("-A", "INPUT", "-p", "icmp", "-j", "DROP");
    }

    @Test
    void buildCommandArgs_forwardChain_usesInputInterface() {
        FirewallRule rule = buildRule(
                FirewallRule.Chain.FORWARD, FirewallRule.Protocol.TCP,
                null, null, null, 443,
                FirewallRule.Action.ACCEPT, "ogstun");

        List<String> args = executor.buildCommandArgs(rule, IptablesExecutor.Operation.APPEND);

        assertThat(args).contains("-i", "ogstun");
    }

    @Test
    void buildCommandArgs_outputChain_usesOutputInterface() {
        FirewallRule rule = buildRule(
                FirewallRule.Chain.OUTPUT, FirewallRule.Protocol.TCP,
                null, null, null, 80,
                FirewallRule.Action.LOG, "eth0");

        List<String> args = executor.buildCommandArgs(rule, IptablesExecutor.Operation.APPEND);

        assertThat(args).contains("-o", "eth0");
        assertThat(args).contains("-j", "LOG");
    }

    @Test
    void buildCommandString_returnsFullCommand() {
        FirewallRule rule = buildRule(
                FirewallRule.Chain.INPUT, FirewallRule.Protocol.TCP,
                null, null, null, 22,
                FirewallRule.Action.ACCEPT, null);

        String cmd = executor.buildCommandString(rule, IptablesExecutor.Operation.APPEND);

        assertThat(cmd).isEqualTo("iptables -A INPUT -p tcp --dport 22 -j ACCEPT");
    }

    @Test
    void execute_dryRunMode_returnsSuccessWithDryRunPrefix() {
        FirewallRule rule = buildRule(
                FirewallRule.Chain.INPUT, FirewallRule.Protocol.TCP,
                null, null, null, 80,
                FirewallRule.Action.ACCEPT, null);

        IptablesExecutor.ExecResult result = executor.execute(rule, IptablesExecutor.Operation.APPEND);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).startsWith("[DRY-RUN]");
        assertThat(result.stdout()).contains("iptables -A INPUT -p tcp --dport 80 -j ACCEPT");
    }

    @Test
    void execute_dryRunMode_deleteOperation_containsMinusD() {
        FirewallRule rule = buildRule(
                FirewallRule.Chain.FORWARD, FirewallRule.Protocol.UDP,
                "10.45.0.0/16", null, null, 2152,
                FirewallRule.Action.ACCEPT, "ogstun");

        IptablesExecutor.ExecResult result = executor.execute(rule, IptablesExecutor.Operation.DELETE);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.stdout()).contains("-D FORWARD");
    }

    @Test
    void isApplyEnabled_defaultFalse() {
        assertThat(executor.isApplyEnabled()).isFalse();
    }

    @Test
    void buildCommandArgs_blankSourceIp_treatedAsNull() {
        FirewallRule rule = buildRule(
                FirewallRule.Chain.INPUT, FirewallRule.Protocol.TCP,
                "  ", null, null, 443,
                FirewallRule.Action.ACCEPT, null);

        List<String> args = executor.buildCommandArgs(rule, IptablesExecutor.Operation.APPEND);

        assertThat(args).doesNotContain("-s");
    }

    @Test
    void buildCommandArgs_zeroPort_omitsPort() {
        FirewallRule rule = buildRule(
                FirewallRule.Chain.INPUT, FirewallRule.Protocol.TCP,
                null, null, 0, 0,
                FirewallRule.Action.DROP, null);

        List<String> args = executor.buildCommandArgs(rule, IptablesExecutor.Operation.APPEND);

        assertThat(args).doesNotContain("--sport");
        assertThat(args).doesNotContain("--dport");
    }

    // ── Helper ──────────────────────────────────────────────────────────

    private FirewallRule buildRule(FirewallRule.Chain chain, FirewallRule.Protocol protocol,
                                  String srcIp, String dstIp,
                                  Integer srcPort, Integer dstPort,
                                  FirewallRule.Action action, String iface) {
        FirewallRule r = new FirewallRule();
        r.setChain(chain);
        r.setProtocol(protocol);
        r.setSourceIp(srcIp);
        r.setDestinationIp(dstIp);
        r.setSourcePort(srcPort);
        r.setDestinationPort(dstPort);
        r.setAction(action);
        r.setInterfaceName(iface);
        r.setDescription("test rule");
        r.setEnabled(true);
        return r;
    }
}
