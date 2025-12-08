/**
 * Post-process script to add @ts-nocheck to generated OpenAPI files.
 * Uses glob patterns to safely iterate files without path traversal risks.
 */

import { readFile, writeFile } from "fs/promises";
import { glob } from "glob";
import { fileURLToPath } from "url";
import { dirname, join } from "path";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const GENERATED_DIR = join(__dirname, "..", "src", "api", "generated");
const TS_NOCHECK = "// @ts-nocheck\n";

async function processGeneratedFiles() {
    // Use glob with explicit pattern - no user input involved
    const pattern = join(GENERATED_DIR, "**", "*.ts").replace(/\\/g, "/");
    const files = await glob(pattern, { nodir: true });

    for (const filePath of files) {
        const content = await readFile(filePath, "utf-8");

        // Skip if already has @ts-nocheck
        if (!content.startsWith("// @ts-nocheck")) {
            await writeFile(filePath, TS_NOCHECK + content);
            console.log(`Added @ts-nocheck to: ${filePath}`);
        }
    }
}

console.log("Post-processing generated TypeScript files...");
processGeneratedFiles()
    .then(() => console.log("Done!"))
    .catch(console.error);
