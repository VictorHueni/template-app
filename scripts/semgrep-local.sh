 #!/bin/bash
 # Local Semgrep scan with proper exclusions matching CI configuration
 # Usage: ./scripts/semgrep-local.sh

 echo "Running Semgrep with CI-equivalent configuration..."
 echo "Exclusions are documented in .semgrep.yml"
 echo ""

 semgrep \
   --config "p/default" \
   --config "p/owasp-top-ten" \
   --exclude-rule "generic.secrets.security.detected-jwt-token.detected-jwt-token" \
   --exclude-rule "generic.nginx.security.possible-h2c-smuggling.possible-nginx-h2c-smuggling" \
   --exclude-rule "generic.nginx.security.request-host-used.request-host-used" \
   --exclude-rule "java.spring.security.audit.spring-actuator-non-health-enabled.spring-actuator-dangerous-endpoints-enabled" \
   --exclude-rule "java.lang.security.audit.unsafe-reflection.unsafe-reflection" \