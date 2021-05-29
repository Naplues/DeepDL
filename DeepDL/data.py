import os
from io import open
import torch
import collections
from itertools import chain
import numpy as np
from torch.utils.data import Dataset, DataLoader

UNKNOWN_WORD = "<UNK>"
EMPTY_WORD = "<EMP>"
END_SENT = "<EOS>"
END_LINE = "<EOL>"
START_SENT = "<BOS>"
END_DOC = "<EOD>"
SUBWORD_END = "@@"



class MyDataset(Dataset):
    def __init__(self, data, target):
        self.data = data
        self.target = target
    def __getitem__(self, item):
        return self.data[item], self.target[item]

    def __len__(self):
        return len(self.data)

def fetch_dataloaders(params, corpus):
    train_data = corpus.train
    test_data = corpus.test

    train_source = []
    train_target = []
    for i in range(0, len(train_data), 5):
        ths_data = train_data[i] + train_data[i+1] + train_data[i+2] + train_data[i+3] + train_data[i+4]
        ths_target = train_data[i+2]
        train_source.append(ths_data)
        train_target.append(ths_target)
    train_source = np.array(train_source)
    train_target = np.array(train_target)


    test_source = []
    test_target = []
    for i in range(0, len(test_data), 5):
        ths_data = test_data[i] + test_data[i + 1] + test_data[i + 2] + test_data[i + 3] + test_data[i + 4]
        ths_target = train_data[i + 2]
        test_source.append(ths_data)
        test_target.append(ths_target)

    batch_num = len(test_data) // params.eval_batch_size
    for i in range(0, (batch_num + 1) * params.eval_batch_size - len(test_data)):
        test_source.append(test_data[len(test_data) - 1][0:len(test_data[i]) - 1])
        test_target.append(test_data[len(test_data) - 1][1:len(test_data[i])])

    test_source = np.array(test_source)
    test_target = np.array(test_target)



    # for i in range(0, len(train_data)):
    #     ths_data = train_data[i][0:len(train_data[i]) - 1]
    #     ths_target = train_data[i][1:len(train_data[i])]
    #     train_source.append(ths_data)
    #     train_target.append(ths_target)
    # train_source = np.array(train_source)
    # train_target = np.array(train_target)
    #
    # test_source = []
    # test_target = []
    # for i in range(0, len(test_data)):
    #     ths_data = test_data[i][0:len(test_data[i]) - 1]
    #     ths_target = test_data[i][1:len(test_data[i])]
    #     test_source.append(ths_data)
    #     test_target.append(ths_target)
    #
    #
    # batch_num = len(test_data) // params.eval_batch_size
    # for i in range(0, (batch_num+1)*params.eval_batch_size - len(test_data)):
    #     test_source.append(test_data[len(test_data)-1][0:len(test_data[i]) - 1])
    #     test_target.append(test_data[len(test_data)-1][1:len(test_data[i])])

    # test_source = np.array(test_source)
    # test_target = np.array(test_target)

    train_dataset = MyDataset(train_source, train_target)
    test_dataset = MyDataset(test_source, test_target)

    train_loader = DataLoader(train_dataset, batch_size=params.batch_size, drop_last=True)
    test_loader = DataLoader(test_dataset, batch_size=params.eval_batch_size, drop_last=False)

    return train_loader, test_loader

class Dictionary(object):
    def __init__(self):
        self.word_to_id = {}
        # self.id_to_word = {}
        self.id_to_word = []

    def build_dictionary(self, path, threshold=2, debug=True):
        with open(path, 'r') as f:
            linewords = (line.replace("\n", " %s" % END_SENT).split() for line in f)


            counter = collections.Counter(chain.from_iterable(linewords))
        if debug: print('Read data for vocabulary!')

        counter[UNKNOWN_WORD] = 0
        unk_counts = 0
        for word, freq in counter.items():
            if freq < threshold:
                unk_counts += freq
                # del counter[word]  # Cleans up resources. Absolutely necessary for large corpora!
        if unk_counts > 0:
            counter[UNKNOWN_WORD] += unk_counts
        if debug: print('UNKS:', unk_counts)

        counter[EMPTY_WORD] = threshold
        counter[END_LINE] = threshold + 1
        count_pairs = sorted(counter.items(), key=lambda x: (-x[1], x[0]))

        words = [word for word, freq in count_pairs if freq >= threshold]
        word_to_id = dict(zip(words, range(len(words))))

        id_to_word = {v: k for k, v in word_to_id.items()}
        self.word_to_id = word_to_id
        self.id_to_word = id_to_word
        return word_to_id, id_to_word

    def add_word(self, word):
        if word not in self.word_to_id:
            self.id_to_word.append(word)
            self.word_to_id[word] = len(self.id_to_word) - 1

    def __len__(self):
        return len(self.id_to_word)


class Corpus(object):
    def __init__(self, path, train_path, test_path, max_length):
        self.dictionary = Dictionary()
        self.dictionary.word_to_id, self.dictionary.id_to_word = Dictionary().build_dictionary(os.path.join(path, train_path))
        self.train = self.tokenize_train_new(os.path.join(path, train_path), max_length)
        self.test = self.tokenize_test_new(os.path.join(path, test_path), max_length)





    def file_to_word_ids(self, path, word_to_id):
        with open(path, 'r') as f:
            ids = []
            for line in f:
                line = line.replace("\n", (" %s" % END_SENT))
                ids.extend([word_to_id[word]
                            if word in word_to_id else word_to_id[UNKNOWN_WORD] for word in line.split()])
        return ids

    # def tokenize(self, path):
    #     assert os.path.exists(path)
    #     ids = self.file_to_word_ids(path, self.dictionary)
    #     idss = torch.tensor(ids)
    #     return idss

    # def tokenize_train(self, path):
    #     """Tokenizes a text file."""
    #     assert os.path.exists(path)
    #     # Add words to the dictionary
    #     with open(path, 'r', encoding="utf8") as f:
    #         for line in f:
    #             line = line.replace("\n", (" %s" % END_SENT))
    #             for word in line.split():
    #                 self.dictionary.add_word(word)
    #
    #     # Tokenize file content
    #     with open(path, 'r', encoding="utf8") as f:
    #         idss = []
    #         for line in f:
    #             line = line.replace("\n", (" %s" % END_SENT))
    #             ids = []
    #             # ids.append([self.dictionary.word_to_id[word]
    #             #             if word in self.dictionary.word_to_id else self.dictionary.word_to_id[UNKNOWN_WORD] for word in line.split()])
    #             for word in line.split():
    #                 ids.append(self.dictionary.word_to_id[word])
    #             idss.append(torch.tensor(ids).type(torch.long))
    #         ids = torch.cat(idss)
    #     return ids



    def tokenize_train(self, path, line_length):
        """Tokenizes a text file."""
        assert os.path.exists(path)
        with open(path, 'r', encoding="utf8") as f:
            idss = []
            line_num = 1
            for line in f:
                if line_num <= 4:
                    line = line.split()
                    ids = []
                    for word in line:
                        if (word in self.dictionary.word_to_id):
                            ids.append(self.dictionary.word_to_id[word])
                        else:
                            ids.append(self.dictionary.word_to_id['<UNK>'])
                    # ids.append(corpus.dictionary.word_to_id[word])
                    if len(ids) < line_length:
                        for i in range(0, line_length - len(ids) - 1):
                            ids.append(self.dictionary.word_to_id['<EMP>'])
                    elif len(ids) > line_length:
                        ids = ids[0:line_length - 1]
                    ids.append(self.dictionary.word_to_id['<EOL>'])

                    idss.append(ids)
                    line_num += 1

                if line_num == 5:
                    line = line.split()
                    ids = []
                    for word in line:
                        if (word in self.dictionary.word_to_id):
                            ids.append(self.dictionary.word_to_id[word])
                        else:
                            ids.append(self.dictionary.word_to_id['<UNK>'])
                    # ids.append(corpus.dictionary.word_to_id[word])
                    if len(ids) < line_length:
                        for i in range(0, line_length - len(ids) - 1):
                            ids.append(self.dictionary.word_to_id['<EMP>'])
                    elif len(ids) > line_length:
                        ids = ids[0:line_length - 1]
                    ids.append(self.dictionary.word_to_id['<EOS>'])
                    idss.append(ids)
                    line_num = 1
            idss = np.array(idss)
        return idss

    def tokenize_test(self, path, line_length):
        """Tokenizes a text file."""
        assert os.path.exists(path)
        with open(path, 'r', encoding="utf8") as f:
            idss = []
            line_num = 1
            for line in f:
                if line_num <= 4:
                    line = line.split()
                    ids = []
                    for word in line:
                        if (word in self.dictionary.word_to_id):
                            ids.append(self.dictionary.word_to_id[word])
                        else:
                            ids.append(self.dictionary.word_to_id['<UNK>'])
                    # ids.append(corpus.dictionary.word_to_id[word])
                    if len(ids) < line_length:
                        for i in range(0, line_length - len(ids) - 1):
                            ids.append(self.dictionary.word_to_id['<EMP>'])
                    elif len(ids) > line_length:
                        ids = ids[0:line_length - 1]
                    ids.append(self.dictionary.word_to_id['<EOL>'])

                    idss.append(ids)
                    line_num += 1

                if line_num == 5:
                    line = line.split()
                    ids = []
                    for word in line:
                        if (word in self.dictionary.word_to_id):
                            ids.append(self.dictionary.word_to_id[word])
                        else:
                            ids.append(self.dictionary.word_to_id['<UNK>'])
                    # ids.append(corpus.dictionary.word_to_id[word])
                    if len(ids) < line_length:
                        for i in range(0, line_length - len(ids) - 1):
                            ids.append(self.dictionary.word_to_id['<EMP>'])
                    elif len(ids) > line_length:
                        ids = ids[0:line_length - 1]
                    ids.append(self.dictionary.word_to_id['<EOS>'])
                    idss.append(ids)
                    line_num = 1
            idss = np.array(idss)
        return idss




    def tokenize_train_new(self, path, max_length):
        """Tokenizes a text file."""
        assert os.path.exists(path)
        # Tokenize file content
        with open(path, 'r', encoding="utf8") as f:
            idss = []
            for line in f:
                line = line.split() + ['<EOS>']
                ids = []
                for word in line:
                    if (word in self.dictionary.word_to_id):
                        ids.append(self.dictionary.word_to_id[word])
                    else:
                        ids.append(self.dictionary.word_to_id['<UNK>'])
                # ids.append(corpus.dictionary.word_to_id[word])
                if len(ids) < max_length:
                    for i in range(0, max_length - len(ids)):
                        ids.append(self.dictionary.word_to_id['<EMP>'])
                elif len(ids) > max_length:
                    ids = ids[0:max_length]
            #     idss.append(torch.tensor(ids).type(torch.long))
            # idsss = torch.cat(idss)
            # return idsss
                idss.append(ids)
            idss = np.array(idss)
        return idss

    def tokenize_train_context(self, path, max_length):
        """Tokenizes a text file."""
        assert os.path.exists(path)
        # Tokenize file content
        commit = 1
        with open(path, 'r', encoding="utf8") as f:
            idss = []
            commit_context = []

            for line in f:
                if commit == 5:
                    # line = line.split() + ['<EOS>']
                    line = line.split()
                    if len(line) > int(max_length / 5):
                        line = line[0:int(max_length / 5) - 1] + ['<EOS>']
                    else:
                        line = line + ['<EOS>']
                    for word in line:
                        if (word in self.dictionary.word_to_id):
                            commit_context.append(self.dictionary.word_to_id[word])
                        else:
                            commit_context.append(self.dictionary.word_to_id['<UNK>'])
                    if len(commit_context) < max_length:
                        for i in range(0, max_length - len(commit_context)):
                            commit_context.append(self.dictionary.word_to_id['<EMP>'])
                    elif len(commit_context) > max_length:
                        commit_context = commit_context[0:max_length]
                    idss.append(commit_context)
                    commit_context = []
                    commit = 1
                else:
                    # line = line.split() + ['<EOL>']
                    line = line.split()
                    if len(line) > int(max_length / 5):
                        line = line[0:int(max_length / 5) - 1] + ['<EOL>']
                    else:
                        line = line + ['<EOL>']
                    for word in line:
                        if (word in self.dictionary.word_to_id):
                            commit_context.append(self.dictionary.word_to_id[word])
                        else:
                            commit_context.append(self.dictionary.word_to_id['<UNK>'])
                    commit += 1

            idss = np.array(idss)
        return idss


    # def tokenize_test(self, path):
    #     """Tokenizes a text file."""
    #     assert os.path.exists(path)
    #     # Tokenize file content
    #     with open(path, 'r', encoding="utf8") as f:
    #         idss = []
    #         for line in f:
    #             line = line.replace("\n", (" %s" % END_SENT))
    #             ids = []
    #             # ids.append([self.dictionary.word_to_id[word]
    #             #             if word in self.dictionary.word_to_id else self.dictionary.word_to_id[UNKNOWN_WORD] for word in line.split()])
    #             for word in line.split():
    #                 ids.append(self.dictionary.word_to_id[word])
    #             idss.append(torch.tensor(ids).type(torch.long))
    #         ids = torch.cat(idss)
    #     return ids

    def tokenize_test_new(self, path, max_length):
        """Tokenizes a text file."""
        assert os.path.exists(path)
        # Tokenize file content
        with open(path, 'r', encoding="utf8") as f:
            idss = []
            for line in f:
                line = line.split() + ['<EOS>']
                ids = []
                for word in line:
                    if(word in self.dictionary.word_to_id):
                        ids.append(self.dictionary.word_to_id[word])
                    else:
                        ids.append(self.dictionary.word_to_id['<UNK>'])
                if len(ids) < max_length:
                    for i in range(0, max_length - len(ids)):
                        ids.append(self.dictionary.word_to_id['<EMP>'])
                elif len(ids) > max_length:
                    ids = ids[0:max_length]
        #         idss.append(torch.tensor(ids).type(torch.long))
        #     idsss = torch.cat(idss)
        # return idsss
                idss.append(ids)
            idss = np.array(idss)
        return idss

    def tokenize_test_context(self, path, max_length):
        """Tokenizes a text file."""
        assert os.path.exists(path)
        # Tokenize file content
        commit = 1
        with open(path, 'r', encoding="utf8") as f:
            idss = []
            commit_context = []

            for line in f:
                if commit == 5:
                    line = line.split()
                    if len(line) > int(max_length / 5):
                        line = line[0:int(max_length / 5)-1] + ['<EOS>']
                    else:
                        line = line + ['<EOS>']

                    for word in line:
                        if (word in self.dictionary.word_to_id):
                            commit_context.append(self.dictionary.word_to_id[word])
                        else:
                            commit_context.append(self.dictionary.word_to_id['<UNK>'])
                    if len(commit_context) < max_length:
                        for i in range(0, max_length - len(commit_context)):
                            commit_context.append(self.dictionary.word_to_id['<EMP>'])

                    elif len(commit_context) > max_length:
                        commit_context = commit_context[0:max_length]
                    idss.append(commit_context)
                    commit_context = []
                    commit = 1
                else:
                    line = line.split()
                    if len(line) > int(max_length / 5):
                        line = line[0:int(max_length / 5) - 1] + ['<EOL>']
                    else:
                        line = line + ['<EOL>']
                    for word in line:
                        if (word in self.dictionary.word_to_id):
                            commit_context.append(self.dictionary.word_to_id[word])
                        else:
                            commit_context.append(self.dictionary.word_to_id['<UNK>'])
                    commit += 1

            idss = np.array(idss)
        return idss


def main():
    corpus = Corpus('/home/qiufangcheng/workspace/OVCNLM/data/dataset/activemq/', 'activemq_clean_train.java', 'activemq_test.java')
    print(corpus.train.shape)

if __name__ == '__main__':
    main()

