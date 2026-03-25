package com.opticoms.optinmscore.domain.firewall.service;

import com.opticoms.optinmscore.domain.firewall.model.FirewallRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Executes iptables commands on the host OS.
 * When {@code firewall.apply-enabled} is false (default for dev),
 * commands are only logged, not executed.
 */
@Slf4j
@Component
public class IptablesExecutor {

    @Value("${firewall.apply-enabled:false}")
    private boolean applyEnabled;

    /**
     * Builds the iptables command arguments for the given rule.
     * The returned list does NOT include the "iptables" binary itself.
     */
    public List<String> buildCommandArgs(FirewallRule rule, Operation op) {
        validatePortProtocol(rule);

        List<String> args = new ArrayList<>();

        args.add(op == Operation.APPEND ? "-A" : "-D");
        args.add(rule.getChain().name());

        if (rule.getProtocol() != FirewallRule.Protocol.ALL) {
            args.add("-p");
            args.add(rule.getProtocol().name().toLowerCase(Locale.ROOT));
        }

        if (rule.getSourceIp() != null && !rule.getSourceIp().isBlank()) {
            args.add("-s");
            args.add(rule.getSourceIp());
        }

        if (rule.getDestinationIp() != null && !rule.getDestinationIp().isBlank()) {
            args.add("-d");
            args.add(rule.getDestinationIp());
        }

        if (rule.getSourcePort() != null && rule.getSourcePort() > 0) {
            args.add("--sport");
            args.add(String.valueOf(rule.getSourcePort()));
        }

        if (rule.getDestinationPort() != null && rule.getDestinationPort() > 0) {
            args.add("--dport");
            args.add(String.valueOf(rule.getDestinationPort()));
        }

        if (rule.getInterfaceName() != null && !rule.getInterfaceName().isBlank()) {
            if (rule.getChain() == FirewallRule.Chain.INPUT || rule.getChain() == FirewallRule.Chain.FORWARD) {
                args.add("-i");
            } else {
                args.add("-o");
            }
            args.add(rule.getInterfaceName());
        }

        if (rule.getOutInterfaceName() != null && !rule.getOutInterfaceName().isBlank()) {
            if (rule.getChain() == FirewallRule.Chain.FORWARD) {
                args.add("-o");
                args.add(rule.getOutInterfaceName());
            }
        }

        args.add("-j");
        args.add(rule.getAction().name());

        return args;
    }

    /**
     * Builds the full command string for logging/display purposes.
     */
    public String buildCommandString(FirewallRule rule, Operation op) {
        List<String> args = buildCommandArgs(rule, op);
        return "iptables " + String.join(" ", args);
    }

    /**
     * Executes the iptables command on the OS.
     *
     * @return {@link ExecResult} with exit code, stdout, and stderr.
     */
    public ExecResult execute(FirewallRule rule, Operation op) {
        String cmd = buildCommandString(rule, op);

        if (!applyEnabled) {
            log.info("[DRY-RUN] {}", cmd);
            return new ExecResult(0, "[DRY-RUN] " + cmd, "");
        }

        log.info("Executing: {}", cmd);
        try {
            List<String> fullCommand = new ArrayList<>();
            fullCommand.add("iptables");
            fullCommand.addAll(buildCommandArgs(rule, op));

            ProcessBuilder pb = new ProcessBuilder(fullCommand);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            String stdout;
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                stdout = reader.lines().collect(Collectors.joining("\n"));
            }

            String stderr;
            try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                stderr = reader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("iptables failed (exit {}): {}", exitCode, stderr);
            }

            return new ExecResult(exitCode, stdout, stderr);
        } catch (Exception e) {
            log.error("Failed to execute iptables command: {}", cmd, e);
            return new ExecResult(-1, "", e.getMessage());
        }
    }

    private void validatePortProtocol(FirewallRule rule) {
        boolean hasPort = (rule.getSourcePort() != null && rule.getSourcePort() > 0)
                || (rule.getDestinationPort() != null && rule.getDestinationPort() > 0);
        if (hasPort && (rule.getProtocol() == FirewallRule.Protocol.ALL
                || rule.getProtocol() == FirewallRule.Protocol.ICMP)) {
            throw new IllegalArgumentException(
                    "Port (--sport/--dport) requires protocol TCP or UDP, but got: " + rule.getProtocol());
        }
    }

    public boolean isApplyEnabled() {
        return applyEnabled;
    }

    public enum Operation {
        APPEND, DELETE
    }

    public record ExecResult(int exitCode, String stdout, String stderr) {
        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
}
