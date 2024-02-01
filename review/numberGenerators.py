# Just return list of natural numbers up to the given limit
def generate_numbers_for_given_limit(limit):
    count = 0
    while count < limit:
        yield count
        count += 1


def print_limit_values():
    limit = 5
    for value in generate_numbers_for_given_limit(limit):
        print("Values within the given limit are %d" % value)


if __name__ == '__main__':
    print_limit_values()
