const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function main() {
    try {
        const count = await prisma.user.count();
        console.log(`Prisma is working! Total users: ${count}`);
    } catch (e) {
        console.error("Prisma error:", e);
    } finally {
        await prisma.$disconnect();
    }
}

main();
