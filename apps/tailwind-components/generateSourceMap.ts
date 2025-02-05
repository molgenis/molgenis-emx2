import { fileURLToPath } from 'url';
import { resolve, join } from 'path';
import fs from 'fs/promises';

const __dirname: string = fileURLToPath(new URL('.', import.meta.url));
const srcDir: string = resolve(__dirname, '.');
const pagesDir: string = join(srcDir, 'pages');
const outputFile: string = resolve(__dirname, 'sourceCodeMap.json');

interface SourceCodeMap {
    [relativePath: string]: string;
}

const sourceCodeMap: SourceCodeMap = {};

// Scan files in the directory and generate source map
const scanDir = async (dir: string): Promise<void> => {
    try {
        const files: string[] = await fs.readdir(dir);
        for (const file of files) {
            const filePath: string = join(dir, file);
            let stat: fs.stat;
            try {
                stat = await fs.stat(filePath);
            } catch (statError: any) {
                console.error(`Error getting stats for ${filePath}:`, statError);
                continue;
            }
            if (stat.isDirectory()) {
                await scanDir(filePath);
            } else if (file.endsWith('.vue')) {
                try {
                    const fileContent: string = await fs.readFile(filePath, 'utf-8');
                    const relativePath: string = filePath.replace(pagesDir, '');
                    sourceCodeMap[relativePath] = fileContent;
                } catch (readError: any) {
                    console.error(`Error reading file ${filePath}:`, readError);
                }
            }
        }
        console.log('✅ Source map generated ...');
    } catch (err: any) {
        console.error(`Error reading directory ${dir}:`, err);
    }
};

// Run the file scanning and write the map to a JSON file
const generateSourceMap = async (): Promise<void> => {
    await scanDir(pagesDir);
    console.log('✅ Writing to file...');
    try {
        await fs.writeFile(outputFile, JSON.stringify(sourceCodeMap, null, 2));
        console.log('✅ Source code map written to sourceCodeMap.json');
    } catch (err: any) {
        console.error('❌ Error writing source code map to file:', err);
    }
};

// Start the generation process
generateSourceMap();