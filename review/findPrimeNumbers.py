import math


def is_prime(number):
    for divider in range(2, math.isqrt(number) + 1):
        if number % divider == 0:
            return False
    return True


def count_prime_number(range_end):
    not_prime = [False] * range_end
    count = 0
    for i in range(2, range_end):
        if not not_prime[i]:
            count += 1
            j = 2
            while i * j < range_end:
                not_prime[i * j] = True
                j += 1
    return count


if __name__ == "__main__":
    print(f"Count prime {count_prime_number(10)}")
