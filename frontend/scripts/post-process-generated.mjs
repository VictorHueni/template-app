/**
 * Post-process script to add @ts-nocheck to generated OpenAPI files
 * This is necessary because the OpenAPI generator creates code with
 * unused imports/parameters that violate strict TypeScript settings.
 */

import { readdir, readFile, writeFile } from "fs/promises";
import { join, resolve, relative, isAbsolute } from "path";
import { fileURLToPath } from "url";
import { dirname } from "path";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const GENERATED_DIR = join(__dirname, "..", "src", "api", "generated");
const TS_NOCHECK = "// @ts-nocheck\n";

function isInsideGeneratedDir(targetPath) {
    const relativePath = relative(GENERATED_DIR, targetPath);
    return relativePath === "" || (!relativePath.startsWith("..") && !isAbsolute(relativePath));
}

async function processDirectory(dir) {
    const entries = await readdir(dir, { withFileTypes: true });

    for (const entry of entries) {
        const fullPath = resolve(dir, entry.name);

        if (!isInsideGeneratedDir(fullPath)) {
            console.warn(`Skipping entry outside generated dir: ${fullPath}`);
            continue;
        }

        if (entry.isDirectory()) {
            await processDirectory(fullPath);
        } else if (entry.name.endsWith(".ts")) {
            const content = await readFile(fullPath, "utf-8");

            // Skip if already has @ts-nocheck
            if (!content.startsWith("// @ts-nocheck")) {
                await writeFile(fullPath, TS_NOCHECK + content);
                console.log(`Added @ts-nocheck to: ${fullPath}`);
            }
        }
    }
}

console.log("Post-processing generated TypeScript files...");
processDirectory(GENERATED_DIR)
    .then(() => console.log("Done!"))
    .catch(console.error);
