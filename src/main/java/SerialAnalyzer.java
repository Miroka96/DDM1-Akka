import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class SerialAnalyzer {

    private static int maxPassword = 1000000;
    private static int prefixLength = 5;

    public static void main(String[] args) throws IOException {
        (new SerialAnalyzer()).analyze(Files.readAllLines(Paths.get("students.csv")));
    }

    public void analyze(List<String> lines) {
        List<String> names = new ArrayList<>(42);
        List<String> secrets = new ArrayList<>(42);
        List<String> sequences = new ArrayList<>(42);

        boolean first = true;
        for (String line : lines) {
            if (first) {
                first = false;
                continue;
            }
            if (line.length() < 1) continue;
            System.out.println(line);
            String[] lineSplit = line.split(";");
            names.add(lineSplit[1]);
            secrets.add(lineSplit[2]);
            sequences.add(lineSplit[3]);
        }

        long t = System.currentTimeMillis();
        int[] cleartexts = this.decrypt(secrets);
        System.out.println("Decryption: " + (System.currentTimeMillis() - t));

        t = System.currentTimeMillis();
        int[] prefixes = this.solveDp(cleartexts);
        System.out.println("Linear Combination: " + (System.currentTimeMillis() - t));

        t = System.currentTimeMillis();
        int[] partners = this.match(sequences);
        System.out.println("Substring: " + (System.currentTimeMillis() - t));

        t = System.currentTimeMillis();
        List<String> hashes = this.encrypt(partners, prefixes, prefixLength);
        System.out.println("Encryption: " + (System.currentTimeMillis() - t));

        for (int i = 0; i < names.size(); i++)
            System.out.println((i + 1) + ";" + names.get(i) + ";" + cleartexts[i] + ";" + prefixes[i] + ";" + (partners[i] + 1) + ";" + hashes.get(i));
    }

    private int[] decrypt(List<String> secrets) {
        int[] cleartexts = new int[secrets.size()];
        for (int i = 0; i < secrets.size(); i++)
            cleartexts[i] = this.unhash(secrets.get(i));
        return cleartexts;
    }

    private int unhash(String hexHash) {
        for (int i = 0; i < maxPassword; i++)
            if (this.hash(i).equals(hexHash))
                return i;
        throw new RuntimeException("Cracking failed for " + hexHash);
    }

    private String hash(int number) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(String.valueOf(number).getBytes(StandardCharsets.UTF_8));

            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < hashedBytes.length; i++) {
                stringBuffer.append(Integer.toString((hashedBytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            return stringBuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private int[] solveDp(int[] numbers) {
        int sum = 0;
        HashSet<Integer> negativeNumbers = new HashSet<>(numbers.length);
        for (int number : numbers) {
            sum += number;
            negativeNumbers.add(number);
        }
        int arraysize = sum / 2;
        boolean[] jumpingField = new boolean[arraysize];

        outer:
        for (int number : numbers) {
            for (int i = arraysize - 1; i >= 0; i--) {
                if (jumpingField[i]) {
                    if (i + number > arraysize) continue;
                    if (i + number == arraysize) break outer;
                    jumpingField[i + number] = true;
                }
            }
            jumpingField[number] = true;
        }

        HashSet<Integer> positiveNumbers = new HashSet<>(numbers.length);

        // problem, because while going backwards, some numbers could be needed twice
        // --> backtracking needed
        for (int i = arraysize; i > 0; ) {
            for (int number : negativeNumbers) {
                if (jumpingField[arraysize - number]) {
                    i = i - number;
                    positiveNumbers.add(number);
                    negativeNumbers.remove(number);
                    break;
                }
            }
        }
        int[] result = new int[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            if (positiveNumbers.contains(numbers[i])) {
                result[i] = 1;
            } else {
                result[i] = -1;
            }
        }
        return result;
    }

    private int[] solve(int[] numbers) {
        for (long a = 0; a < Long.MAX_VALUE; a++) {
            String binary = Long.toBinaryString(a);

            int[] prefixes = new int[62];
            for (int i = 0; i < prefixes.length; i++)
                prefixes[i] = 1;

            int i = 0;
            for (int j = binary.length() - 1; j >= 0; j--) {
                if (binary.charAt(j) == '1')
                    prefixes[i] = -1;
                i++;
            }

            if (this.sum(numbers, prefixes) == 0) {
                System.out.println(a);
                return prefixes;
            }
        }

        throw new RuntimeException("Prefix not found!");
    }

    private int sum(int[] numbers, int[] prefixes) {
        int sum = 0;
        for (int i = 0; i < numbers.length; i++)
            sum += numbers[i] * prefixes[i];
        return sum;
    }

    private int[] match(List<String> sequences) {
        int[] partners = new int[sequences.size()];
        for (int i = 0; i < sequences.size(); i++)
            partners[i] = this.longestOverlapPartner(i, sequences);
        return partners;
    }

    private int longestOverlapPartner(int thisIndex, List<String> sequences) {
        int bestOtherIndex = -1;
        String bestOverlap = "";
        for (int otherIndex = 0; otherIndex < sequences.size(); otherIndex++) {
            if (otherIndex == thisIndex)
                continue;

            String longestOverlap = this.longestOverlap(sequences.get(thisIndex), sequences.get(otherIndex));

            if (bestOverlap.length() < longestOverlap.length()) {
                bestOverlap = longestOverlap;
                bestOtherIndex = otherIndex;
            }
        }
        return bestOtherIndex;
    }

    private String longestOverlap(String str1, String str2) {
        if (str1.isEmpty() || str2.isEmpty())
            return "";

        if (str1.length() > str2.length()) {
            String temp = str1;
            str1 = str2;
            str2 = temp;
        }

        int[] currentRow = new int[str1.length()];
        int[] lastRow = str2.length() > 1 ? new int[str1.length()] : null;
        int longestSubstringLength = 0;
        int longestSubstringStart = 0;

        for (int str2Index = 0; str2Index < str2.length(); str2Index++) {
            char str2Char = str2.charAt(str2Index);
            for (int str1Index = 0; str1Index < str1.length(); str1Index++) {
                int newLength;
                if (str1.charAt(str1Index) == str2Char) {
                    newLength = str1Index == 0 || str2Index == 0 ? 1 : lastRow[str1Index - 1] + 1;

                    if (newLength > longestSubstringLength) {
                        longestSubstringLength = newLength;
                        longestSubstringStart = str1Index - (newLength - 1);
                    }
                } else {
                    newLength = 0;
                }
                currentRow[str1Index] = newLength;
            }
            int[] temp = currentRow;
            currentRow = lastRow;
            lastRow = temp;
        }
        return str1.substring(longestSubstringStart, longestSubstringStart + longestSubstringLength);
    }

    private List<String> encrypt(int[] partners, int[] prefixes, int prefixLength) {
        List<String> hashes = new ArrayList<>(partners.length);
        for (int i = 0; i < partners.length; i++) {
            int partner = partners[i];
            String prefix = (prefixes[i] > 0) ? "1" : "0";
            hashes.add(this.findHash(partner, prefix, prefixLength));
        }
        return hashes;
    }

    private String findHash(int content, String prefix, int prefixLength) {
        StringBuilder fullPrefixBuilder = new StringBuilder();
        for (int i = 0; i < prefixLength; i++)
            fullPrefixBuilder.append(prefix);

        Random rand = new Random(13);

        String fullPrefix = fullPrefixBuilder.toString();
        int nonce = 0;
        while (true) {
            nonce = rand.nextInt();
            String hash = this.hash(content + nonce);
            if (hash.startsWith(fullPrefix))
                return hash;
        }
    }
}
