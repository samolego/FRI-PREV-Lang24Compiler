package lang24.common;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;


public class StringUtil {

    /**
     * Calculates the Levenshtein distance between two strings.
     * @param x The first string.
     * @param y The second string.
     * @return The Levenshtein distance between the two strings.
     * @source https://www.baeldung.com/java-levenshtein-distance
     */
    public static int calculate(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                }
                else if (j == 0) {
                    dp[i][j] = i;
                }
                else {
                    dp[i][j] = min(dp[i - 1][j - 1]
                                    + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
                            dp[i - 1][j] + 1,
                            dp[i][j - 1] + 1);
                }
            }
        }

        return dp[x.length()][y.length()];
    }

    public static int min(int... numbers) {
        return Arrays.stream(numbers)
          .min().orElse(Integer.MAX_VALUE);
    }

    public static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    public static Optional<String> findSimilar(String original, Iterator<String> defns, int threshold) {
        int minDist = Integer.MAX_VALUE;
        String similar = null;
        while (defns.hasNext()) {
            var cmp = defns.next();

            if (similar == null) {
                similar = cmp;
                minDist = StringUtil.calculate(similar, original);
            } else {
                int dist = StringUtil.calculate(cmp, original);

                if (dist < minDist) {
                    similar = cmp;
                    minDist = dist;
                }
            }
        }

        if (minDist < threshold) {
            return Optional.of(similar);
        } else {
            return Optional.empty();
        }
    }
}
