import { fileURLToPath } from 'url';
import { resolve, join } from 'path';
import fs from 'fs/promises';

// Get the directory name for the current module using import.meta.url
const __dirname = fileURLToPath(new URL('.', import.meta.url));

const srcDir = resolve(__dirname, '.'); // Adjust this path to your `srcDir`
const pagesDir = join(srcDir, 'pages'); // Adjust this path to your `pages` directory
const outputFile = resolve(__dirname, 'sourceCodeMap.json'); // Output path for the generated map

const sourceCodeMap = {};

// Scan files in the directory and generate source map
const scanDir = async (dir) => {
    try {
        const files = await fs.readdir(dir);
        for (const file of files) {
            const filePath = join(dir, file);
            let stat;
            try {
                stat = await fs.stat(filePath);
            } catch (statError) {
                console.error(`Error getting stats for ${filePath}:`, statError);
                continue;
            }
            if (stat.isDirectory()) {
                await scanDir(filePath);
            } else if (file.endsWith('.vue')) {
                try {
                    const fileContent = await fs.readFile(filePath, 'utf-8');
                    const relativePath = filePath.replace(pagesDir, '');
                    sourceCodeMap[relativePath] = fileContent;
                } catch (readError) {
                    console.error(`Error reading file ${filePath}:`, readError);
                }
            }
        }
    } catch (err) {
        console.error(`Error reading directory ${dir}:`, err);
    }
};

// Run the file scanning and write the map to a JSON file
const generateSourceMap = async () => {
    await scanDir(pagesDir);
    console.log('✅ Source map generated, writing to file...');
    try {
        await fs.writeFile(outputFile, JSON.stringify(sourceCodeMap, null, 2));
        console.log('✅ Source code map written to sourceCodeMap.json');
    } catch (err) {
        console.error('❌ Error writing source code map to file:', err);
    }
};

// Start the generation process
generateSourceMap();
